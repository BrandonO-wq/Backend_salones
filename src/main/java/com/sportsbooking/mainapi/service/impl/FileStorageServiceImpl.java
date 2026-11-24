package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.config.VenueProperties;
import com.sportsbooking.mainapi.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación de {@link FileStorageService} para almacenamiento de imágenes en disco.
 *
 * <p>Validaciones:
 * <ul>
 *   <li>Extensiones permitidas: jpg, jpeg, png, webp</li>
 *   <li>Content-type real verificado con {@link ImageIO}</li>
 *   <li>Tamaño máximo configurable (default 5MB)</li>
 * </ul>
 *
 * <p>Optimización:
 * <ul>
 *   <li>Imágenes &gt; 1MB se comprimen a JPEG 80%</li>
 *   <li>Imágenes &gt; 2000px se redimensionan proporcionalmente</li>
 * </ul>
 *
 * <p>Los archivos se guardan en: {@code {base-path}/{venueId}/{uuidv7}.jpg}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final VenueProperties venueProperties;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final int MAX_DIMENSION = 2000;
    private static final int COMPRESSION_THRESHOLD = 1024 * 1024; // 1MB
    private static final float JPEG_QUALITY = 0.80f;

    /**
     * Crea el directorio base al iniciar si no existe.
     */
    @PostConstruct
    void init() {
        try {
            Path basePath = Paths.get(venueProperties.getImages().getBasePath());
            Files.createDirectories(basePath);
            log.info("[FileStorage] Directorio base: {}", basePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[FileStorage] Error creando directorio base: {}", e.getMessage(), e);
        }
    }

    @Override
    public String store(MultipartFile file, UUID venueId) {
        // 1. Validar tamaño
        long maxBytes = venueProperties.getImages().getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "El archivo excede el tamaño máximo de " + venueProperties.getImages().getMaxSizeMb() + "MB");
        }

        // 2. Validar extensión
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Extensión no permitida: " + extension + ". Permitidas: " + ALLOWED_EXTENSIONS);
        }

        // 3. Validar que sea imagen real (previene archivos maliciosos)
        BufferedImage image;
        try (InputStream is = file.getInputStream()) {
            image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error leyendo el archivo: " + e.getMessage());
        }

        // 4. Optimizar: redimensionar si es muy grande
        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            image = resize(image);
        }

        // 5. Generar nombre con UUIDv7
        String filename = UUID.randomUUID() + ".jpg";

        // 6. Comprimir y guardar
        try {
            Path venueDir = Paths.get(venueProperties.getImages().getBasePath(), venueId.toString());
            Files.createDirectories(venueDir);
            Path filePath = venueDir.resolve(filename);

            byte[] optimized = compress(image);
            Files.write(filePath, optimized);

            log.info("[FileStorage] Imagen guardada: {} ({} bytes)", filePath, optimized.length);
            return filename;

        } catch (IOException e) {
            throw new RuntimeException("Error guardando imagen: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource load(UUID venueId, String filename) {
        try {
            Path filePath = Paths.get(venueProperties.getImages().getBasePath(), venueId.toString(), filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("VenuePhoto", filename);
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("VenuePhoto", filename);
        }
    }

    @Override
    public void delete(UUID venueId, String filename) {
        try {
            Path filePath = Paths.get(venueProperties.getImages().getBasePath(), venueId.toString(), filename);
            Files.deleteIfExists(filePath);
            log.info("[FileStorage] Imagen eliminada: {}", filePath);
        } catch (IOException e) {
            log.warn("[FileStorage] No se pudo eliminar la imagen {}: {}", filename, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Métodos privados
    // ─────────────────────────────────────────────

    /**
     * Extrae la extensión de un nombre de archivo.
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Redimensiona proporcionalmente una imagen para que no exceda {@code MAX_DIMENSION}.
     */
    private BufferedImage resize(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        double ratio = Math.min((double) MAX_DIMENSION / w, (double) MAX_DIMENSION / h);
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        log.debug("[FileStorage] Imagen redimensionada de {}x{} a {}x{}", w, h, newW, newH);
        return resized;
    }

    /**
     * Comprime una imagen a JPEG con calidad configurable.
     *
     * @return bytes del JPEG comprimido.
     */
    private byte[] compress(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");

        if (!writers.hasNext()) {
            throw new IOException("No se encontró escritor JPEG");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();

        return baos.toByteArray();
    }
}
