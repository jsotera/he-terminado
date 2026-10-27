<#ftl output_format="HTML" auto_esc=true>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>¡He terminado! · Aula</title>
  <link rel="stylesheet" href="/style.css"><script src="/app.js" defer></script>
</head>
<body data-role="${teacher?then('teacher', 'student')}">
<header><a class="brand" href="/index"><span class="brand-icon">✓</span> ¡He terminado!</a><div class="header-status"><span id="connection" role="status">Conectando…</span><span class="badge">${teacher?then('Profesorado', 'Alumnado')}</span></div></header>
<main>
  <p id="error" class="error" role="alert" hidden></p>
  <#if teacher>
  <div class="teacher-layout"><aside class="teacher-sidebar">
  <section class="stats" aria-label="Resumen del aula"><article><span>Alumnado</span><strong id="student-count">0</strong></article><article><span>Actividades</span><strong id="activity-count">0</strong></article><article><span>Todo terminado</span><strong id="finished-count">0</strong></article></section>
  <section class="panel"><h2>Actividades</h2><form id="activity-form"><label for="activity-title">Nueva actividad</label><div class="inline"><input id="activity-title" maxlength="100" placeholder="Ej. Ejercicio de arrays" required><button type="submit" aria-label="Añadir actividad">+</button></div></form><div id="activity-list"></div><button id="reset" class="secondary full">Reiniciar progreso</button><p class="hint">Conserva los nombres, las actividades y los asientos.</p></section></aside>
  <section class="panel classroom"><div class="section-heading classroom-heading"><h2>Mi aula</h2><label class="activity-filter-control">Ver progreso de <select id="activity-filter"><option value="">Todas las actividades</option></select></label><button id="toggle-classroom" class="secondary" type="button" aria-controls="classroom-map" aria-pressed="false" title="Mostrar solo los pupitres">⤢ Maximizar</button></div>
  <div class="classroom-settings">
  <form id="layout-form" class="layout-form"><label>Filas <input id="rows" type="number" min="1" max="12" value="4" required></label><label>Columnas <input id="columns" type="number" min="1" max="12" value="5" required></label><button class="secondary">Guardar distribución</button></form>
  <div class="classroom-tools"><label class="checkbox-label"><input id="invert-view" type="checkbox"> Invertir vista del aula (pizarra abajo)</label><button id="aisle-tool" class="secondary aisle-tool" type="button" draggable="true" aria-pressed="false">↕ Pasillo</button></div>
  <p class="hint">Arrastra «Pasillo» a un pupitre o pulsa «Pasillo» y elige una casilla. Si está ocupado, el alumno pasa a «Sin asiento». La × recupera el pupitre.</p>
  <div class="legend"><span class="dot red"></span> Pendiente <span class="dot green"></span> Terminado <span class="dot gray"></span> Sin actividades</div><p class="hint">Arrastra a cada alumno a su sitio. También puedes seleccionarlo y pulsar un asiento para moverlo.</p>
  </div>
  <div id="classroom-map" class="classroom-map"><div class="board">PIZARRA</div><div class="grid-scroll"><div id="seats" class="seats"></div></div></div><div class="unseated-section"><h3>Sin asiento</h3><div id="unseated" class="unseated" tabindex="0" role="button" aria-label="Mover el alumno seleccionado a sin asiento"></div><p id="selection" class="hint" aria-live="polite"></p></div>
  </section></div>
  <#else>
  <div class="intro"><div><h1>A tu ritmo. Paso a paso.</h1><p>Marca las actividades que hayas terminado. Tu profesor lo verá al momento.</p></div></div>
  <section id="identity" class="panel student-panel"><h2>¿Cómo te llamas?</h2><p>Usa un nombre que tu profesor reconozca.</p><form id="name-form"><label for="student-name">Tu nombre</label><div class="inline"><input id="student-name" autocomplete="name" maxlength="60" placeholder="Nombre y apellido" required><button>Entrar al aula</button></div></form><button id="cancel-name" class="secondary" hidden>Cancelar</button></section>
  <section id="student-work" class="panel student-panel" hidden><div class="section-heading"><div><p class="eyebrow">TU ESPACIO DE TRABAJO</p><h2 id="greeting"></h2></div><button id="change-name" class="secondary">Cambiar nombre</button></div><p id="student-inactive" class="hint" hidden>El profesorado ha desactivado tu participación temporalmente.</p><p id="student-summary"></p><div id="student-activities"></div><p class="hint">Puedes desmarcar una actividad si necesitas seguir trabajando en ella.</p></section>
  </#if>
  <footer>He terminado! <span>Un pequeño gesto. Una clase conectada.</span></footer>
</main>
</body></html>
