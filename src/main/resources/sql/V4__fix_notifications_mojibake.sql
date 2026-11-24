-- Script para corregir registros históricos con Mojibake (UTF-8 interpretado como Latin-1)
-- en la tabla notifications.

UPDATE notifications 
SET body = REPLACE(REPLACE(REPLACE(REPLACE(body, 'âš ï¸', '⚠️'), 'âš', '⚠️'), 'dÃas', 'días'), 'DÃas', 'Días'),
    title = REPLACE(REPLACE(REPLACE(REPLACE(title, 'âš ï¸', '⚠️'), 'âš', '⚠️'), 'dÃas', 'días'), 'DÃas', 'Días')
WHERE body LIKE '%dÃas%' 
   OR body LIKE '%âš%' 
   OR title LIKE '%dÃas%' 
   OR title LIKE '%âš%';

