// package com.sportsbooking.mainapi.dev;

// import com.sportsbooking.mainapi.dto.venue.request.CreateVenueRequest;
// import com.sportsbooking.mainapi.service.VenueService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Profile;
// import org.springframework.stereotype.Component;

// import java.math.BigDecimal;
// import java.time.LocalTime;
// import java.util.List;
// import java.util.UUID;

// /**
// * Runner usado solo en perfil local para invocar createVenue y capturar
// * errores.
// */
// @Slf4j
// @Component
// @Profile("local")
// @RequiredArgsConstructor
// public class LocalVenueDebugRunner implements CommandLineRunner {

// private final VenueService venueService;

// @Override
// public void run(String... args) throws Exception {
// try {
// CreateVenueRequest req = new CreateVenueRequest(
// "Fuimoss",
// "nzjxjjxjzkx",
// -9.5178952,
// -77.5229815,
// "020105",
// "bdndnxndn",
// null,
// false,
// "973000149",
// List.of(1),
// List.of(),
// List.of(27),
// null,
// null,
// List.of(new CreateVenueRequest.PayWalletItem(19, "Diego", "973000149")),
// List.of(new CreateVenueRequest.VenueParkingItem(300, new
// BigDecimal("200.0"))),
// List.of(new CreateVenueRequest.VenueBathroomItem(500)),
// 50,
// 300,
// null,
// null,
// null,
// null,
// List.of(27),
// List.of(10),
// List.of(32, 31, 29, 30),
// List.of(38, 34, 37, 36, 35, 33),
// List.of(42, 40, 41, 39),
// List.of(44, 45),
// List.of(48, 47, 49, 46),
// List.of(53, 50, 52, 51, 54),
// new BigDecimal("250.0"),
// 4,
// 20,
// new BigDecimal("300.0"),
// LocalTime.parse("00:00"),
// LocalTime.parse("00:00"),
// 30,
// "jdjdjdjdjdjdjd");

// log.info("[LocalDebug] Invocando createVenue de prueba...");
// var resp = venueService.createVenue(UUID.randomUUID(), req);
// log.info("[LocalDebug] createVenue éxito: {}", resp.id());

// } catch (Exception e) {
// log.error("[LocalDebug] Error al ejecutar createVenue de prueba", e);
// // Rethrow to ensure stacktrace is visible on startup
// throw e;
// }
// }
// }
