<#ftl output_format="HTML" auto_esc=true>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>¡He terminado! · Aula</title><link rel="stylesheet" href="/style.css"><script src="/app.js" defer></script>
</head>
<body data-role="${teacher?then('teacher', 'student')}">
<header><a class="brand" href="/index"><span class="brand-icon">✓</span> ¡He terminado!</a><span class="badge">${teacher?then('Profesorado', 'Alumnado')}</span></header>
<main>
  <div class="intro"><div><p class="eyebrow">EL AULA, DE UN VISTAZO</p><h1>${teacher?then('Cada avance cuenta.', 'A tu ritmo. Paso a paso.')}</h1><p>${teacher?then('Organiza las actividades y acompaña el progreso de tu clase.', 'Marca las actividades que hayas terminado. Tu profesor lo verá al momento.')}</p></div><span id="connection" role="status">Conectando…</span></div>
  <p id="error" class="error" role="alert" hidden></p>
  <#if teacher>
  <section class="stats" aria-label="Resumen del aula"><article><span>Alumnado</span><strong id="student-count">0</strong></article><article><span>Actividades</span><strong id="activity-count">0</strong></article><article><span>Todo terminado</span><strong id="finished-count">0</strong></article></section>
  <div class="teacher-layout"><aside class="panel"><h2>Actividades</h2><form id="activity-form"><label for="activity-title">Nueva actividad</label><div class="inline"><input id="activity-title" maxlength="100" placeholder="Ej. Ejercicio de arrays" required><button type="submit" aria-label="Añadir actividad">+</button></div></form><div id="activity-list"></div><button id="reset" class="secondary full">Reiniciar progreso</button><p class="hint">Conserva los nombres, las actividades y los asientos.</p></aside>
  <section class="panel classroom"><div class="section-heading"><h2>Mi aula</h2><label>Ver progreso de <select id="activity-filter"><option value="">Todas las actividades</option></select></label></div>
  <form id="layout-form" class="layout-form"><label>Filas <input id="rows" type="number" min="1" max="12" value="4" required></label><label>Columnas <input id="columns" type="number" min="1" max="12" value="5" required></label><button class="secondary">Guardar distribución</button></form>
  <div class="legend"><span class="dot red"></span> Pendiente <span class="dot green"></span> Terminado <span class="dot gray"></span> Sin actividades</div><p class="hint">Arrastra a cada alumno a su sitio. También puedes seleccionarlo y pulsar un asiento para moverlo.</p>
  <div class="board">PIZARRA</div><div class="grid-scroll"><div id="seats" class="seats"></div></div><h3>Sin asiento</h3><div id="unseated" class="unseated" tabindex="0" role="button" aria-label="Mover el alumno seleccionado a sin asiento"></div><p id="selection" class="hint" aria-live="polite"></p>
  </section></div>
  <#else>
  <section id="identity" class="panel student-panel"><h2>¿Cómo te llamas?</h2><p>Usa un nombre que tu profesor reconozca.</p><form id="name-form"><label for="student-name">Tu nombre</label><div class="inline"><input id="student-name" autocomplete="name" maxlength="60" placeholder="Nombre y apellido" required><button>Entrar al aula</button></div></form><button id="cancel-name" class="secondary" hidden>Cancelar</button></section>
  <section id="student-work" class="panel student-panel" hidden><div class="section-heading"><div><p class="eyebrow">TU ESPACIO DE TRABAJO</p><h2 id="greeting"></h2></div><button id="change-name" class="secondary">Cambiar nombre</button></div><p id="student-summary"></p><div id="student-activities"></div><p class="hint">Puedes desmarcar una actividad si necesitas seguir trabajando en ella.</p></section>
  </#if>
  <footer>He terminado! <span>Un pequeño gesto. Una clase conectada.</span></footer>
</main>
</body></html>
