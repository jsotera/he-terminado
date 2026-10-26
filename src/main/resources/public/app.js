"use strict";
const $ = (id) => document.getElementById(id);
const teacher = document.body.dataset.role === "teacher";
let state,
  selectedStudent = null,
  editingName = false,
  busy = false,
  dragging = false;
let lastView = "";
let placingAisle = false;
function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}
function error(message) {
  $("error").textContent = message;
  $("error").hidden = !message;
}
async function request(path, method = "GET", body) {
  const response = await fetch(path, {
    method,
    headers: method === "GET" ? {} : { "Content-Type": "application/json" },
    body: method === "GET" ? undefined : JSON.stringify(body ?? {}),
  });
  if (!response.ok) {
    const details = await response.json().catch(() => ({}));
    throw new Error(details.message || "No se pudo conectar con el servidor.");
  }
  return response.status === 204 ? null : response.json();
}
async function refresh() {
  if (busy || dragging) return;
  try {
    const next = await request("/api/state");
    state = next;
    const signature = JSON.stringify(next);
    if (signature !== lastView) {
      render();
      lastView = signature;
    }
    $("connection").textContent = "● Conectado · cada 2 s";
  } catch (e) {
    $("connection").textContent = "● Sin conexión · reintentando";
    error(e.message);
  }
}
async function mutate(path, method, body) {
  if (busy) return false;
  busy = true;
  error("");
  try {
    await request(path, method, body);
    return true;
  } catch (e) {
    error(e.message);
    return false;
  } finally {
    busy = false;
    lastView = "";
    await refresh();
  }
}
function render() {
  if (teacher) renderTeacher();
  else renderStudent();
}
function renderTeacher() {
  const c = state.classroom;
  $("student-count").textContent = c.students.length;
  $("activity-count").textContent = c.activities.length;
  $("finished-count").textContent = c.activities.length
    ? c.students.filter((s) =>
        c.activities.every((a) => s.completed.includes(a.id)),
      ).length
    : 0;
  if (
    document.activeElement !== $("rows") &&
    document.activeElement !== $("columns")
  ) {
    $("rows").value = c.rows;
    $("columns").value = c.columns;
  }
  const filter = $("activity-filter"),
    oldFilter = filter.value;
  filter.replaceChildren(new Option("Todas las actividades", ""));
  c.activities.forEach((a) => filter.add(new Option(a.title, a.id)));
  filter.value = c.activities.some((a) => a.id === oldFilter) ? oldFilter : "";
  $("activity-list").replaceChildren();
  if (!c.activities.length)
    $("activity-list").append(
      element("p", "Añade la primera actividad para empezar.", "empty"),
    );
  c.activities.forEach((a) => {
    const row = element("div", undefined, "activity-row");
    row.append(element("span", a.title));
    const remove = element("button", "×", "icon-button");
    remove.type = "button";
    remove.setAttribute("aria-label", "Eliminar " + a.title);
    remove.onclick = () => {
      if (confirm("¿Eliminar «" + a.title + "» y su progreso?"))
        mutate("/api/activities/" + a.id, "DELETE");
    };
    row.append(remove);
    $("activity-list").append(row);
  });
  renderSeats();
}
function selectStudent(s) {
  placingAisle = false;
  selectedStudent = selectedStudent === s.id ? null : s.id;
  renderSeats();
}
function studentCard(s) {
  const activities = state.classroom.activities;
  const filter = $("activity-filter").value;
  const done = filter
    ? s.completed.includes(filter)
    : activities.length > 0 &&
      activities.every((a) => s.completed.includes(a.id));
  const card = element(
    "button",
    undefined,
    "student-card " +
      (!activities.length ? "neutral" : done ? "done" : "pending") +
      (selectedStudent === s.id ? " selected" : ""),
  );
  card.type = "button";
  card.draggable = true;
  card.setAttribute("aria-pressed", String(selectedStudent === s.id));
  card.append(
    element("strong", s.name),
    element(
      "span",
      !activities.length
        ? "Sin actividades"
        : s.completed.length + " de " + activities.length + " terminadas",
      "card-summary",
    ),
  );
  const checklist = element("span", undefined, "card-activities");
  activities.forEach((activity) => {
    const completed = s.completed.includes(activity.id);
    const status = completed ? "Terminada" : "Pendiente";
    const row = element(
      "span",
      undefined,
      "card-activity " +
        (completed ? "activity-done" : "activity-pending") +
        (filter === activity.id ? " activity-focus" : ""),
    );
    row.title = activity.title + ": " + status;
    const check = element("span", completed ? "✓" : "○", "activity-check");
    check.setAttribute("aria-hidden", "true");
    row.append(
      check,
      element("span", activity.title, "activity-title"),
      element("span", ": " + status, "sr-only"),
    );
    checklist.append(row);
  });
  card.append(checklist);
  card.onclick = (event) => {
    event.stopPropagation();
    if (placingAisle && s.seat !== null) {
      placeAisle(s.seat);
      return;
    }
    selectStudent(s);
  };
  card.ondragstart = (event) => {
    dragging = true;
    event.dataTransfer.setData("text/plain", s.id);
    event.dataTransfer.effectAllowed = "move";
  };
  card.ondragend = () => {
    dragging = false;
  };
  return card;
}
function dropTarget(node, seat) {
  const move = (id) => {
    if (!id || !state.classroom.students.some((s) => s.id === id)) return;
    selectedStudent = null;
    mutate("/api/seats/" + id, "PUT", { seat });
  };
  node.ondragover = (e) => {
    const aisle = e.dataTransfer.types.includes("application/x-classroom-tool");
    if (aisle && seat === null) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = aisle ? "copy" : "move";
  };
  node.ondrop = (e) => {
    e.preventDefault();
    dragging = false;
    if (e.dataTransfer.getData("application/x-classroom-tool") === "aisle") {
      if (seat !== null) placeAisle(seat);
      return;
    }
    move(e.dataTransfer.getData("text/plain"));
  };
  const activate = () => {
    if (placingAisle && seat !== null) placeAisle(seat);
    else move(selectedStudent);
  };
  node.onclick = activate;
  node.onkeydown = (e) => {
    if (e.target === node && (e.key === "Enter" || e.key === " ")) {
      e.preventDefault();
      activate();
    }
  };
}
function placeAisle(seat) {
  placingAisle = false;
  selectedStudent = null;
  mutate("/api/aisles/" + seat, "PUT", { aisle: true });
}
function aisleCell(seat) {
  const node = element("div", undefined, "aisle");
  node.dataset.seat = seat;
  node.append(element("span", "Pasillo", "aisle-label"));
  const remove = element("button", "×", "icon-button");
  remove.type = "button";
  remove.setAttribute("aria-label", "Quitar pasillo y recuperar pupitre");
  remove.onclick = () => mutate("/api/aisles/" + seat, "PUT", { aisle: false });
  node.append(remove);
  // No añadimos dropTarget: un pasillo no admite alumnado.
  return node;
}
function renderSeats() {
  const c = state.classroom;
  const seats = $("seats");
  seats.replaceChildren();
  // La plantilla de columnas se expresa con clases para mantener una CSP sin estilos inline.
  seats.className = "seats columns-" + c.columns;
  $("invert-view").checked = c.inverted;
  $("aisle-tool").setAttribute("aria-pressed", String(placingAisle));
  $("classroom-map").classList.toggle("inverted", c.inverted);
  for (let position = 0; position < c.rows * c.columns; position++) {
    // Reordenamos solo el dibujo; cada alumno conserva su identificador de asiento.
    const i = c.inverted ? c.rows * c.columns - 1 - position : position;
    if (c.aisles.includes(i)) {
      seats.append(aisleCell(i));
      continue;
    }
    const node = element("div", undefined, "seat");
    node.dataset.seat = i;
    node.tabIndex = 0;
    node.setAttribute("role", "group");
    node.setAttribute(
      "aria-label",
      "Fila " +
        (Math.floor(i / c.columns) + 1) +
        ", columna " +
        ((i % c.columns) + 1),
    );
    node.append(
      element(
        "span",
        Math.floor(i / c.columns) + 1 + " · " + ((i % c.columns) + 1),
        "seat-number",
      ),
    );
    const student = c.students.find((s) => s.seat === i);
    if (student) node.append(studentCard(student));
    else node.append(element("span", "Libre", "free"));
    dropTarget(node, i);
    seats.append(node);
  }
  const unseated = $("unseated");
  unseated.replaceChildren();
  c.students
    .filter((s) => s.seat === null)
    .forEach((s) => unseated.append(studentCard(s)));
  if (!unseated.childElementCount)
    unseated.append(element("p", "Sin alumnado pendiente de colocar.", "hint"));
  dropTarget(unseated, null);
  const selected = c.students.find((s) => s.id === selectedStudent);
  $("selection").textContent = placingAisle
    ? "Pasillo seleccionado: pulsa un pupitre para convertirlo en pasillo."
    : selected
      ? selected.name +
        ": pulsa el asiento de destino o su nombre para cancelar."
      : "";
}
function renderStudent() {
  const s = state.student;
  $("identity").hidden = !!s && !editingName;
  $("student-work").hidden = !s || editingName;
  $("cancel-name").hidden = !s;
  if (!s) return;
  $("greeting").textContent = "Hola, " + s.name;
  $("student-summary").textContent = state.activities.length
    ? s.completed.length +
      " de " +
      state.activities.length +
      " actividades terminadas"
    : "Tu profesor todavía no ha añadido actividades.";
  const list = $("student-activities");
  list.replaceChildren();
  state.activities.forEach((a) => {
    const done = s.completed.includes(a.id);
    const label = element(
      "label",
      undefined,
      "task " + (done ? "done" : "pending"),
    );
    const input = element("input");
    input.type = "checkbox";
    input.checked = done;
    input.onchange = () => {
      mutate("/api/progress/" + a.id, "PUT", { completed: input.checked });
    };
    const text = element("div");
    text.append(
      element("strong", a.title),
      element("small", done ? "✓ Terminado" : "Pendiente"),
    );
    label.append(input, text);
    list.append(label);
  });
}
if (teacher) {
  $("invert-view").onchange = async () => {
    const checkbox = $("invert-view");
    checkbox.disabled = true;
    try {
      await mutate("/api/view", "PUT", { inverted: checkbox.checked });
    } finally {
      checkbox.disabled = false;
      renderSeats();
    }
  };
  $("aisle-tool").onclick = () => {
    placingAisle = !placingAisle;
    selectedStudent = null;
    renderSeats();
  };
  $("aisle-tool").ondragstart = (event) => {
    dragging = true;
    event.dataTransfer.setData("application/x-classroom-tool", "aisle");
    event.dataTransfer.setData("text/plain", "Pasillo");
    event.dataTransfer.effectAllowed = "copy";
  };
  $("aisle-tool").ondragend = () => {
    dragging = false;
  };
  $("activity-form").onsubmit = async (e) => {
    e.preventDefault();
    if (
      await mutate("/api/activities", "POST", {
        title: $("activity-title").value,
      })
    )
      $("activity-title").value = "";
  };
  $("layout-form").onsubmit = (e) => {
    e.preventDefault();
    mutate("/api/layout", "PUT", {
      rows: Number($("rows").value),
      columns: Number($("columns").value),
    });
  };
  $("activity-filter").onchange = renderSeats;
  $("reset").onclick = () => {
    if (
      confirm(
        "¿Marcar todas las actividades como pendientes para todo el alumnado?",
      )
    )
      mutate("/api/reset", "POST");
  };
} else {
  $("name-form").onsubmit = async (e) => {
    e.preventDefault();
    if (
      await mutate("/api/student", "POST", { name: $("student-name").value })
    ) {
      editingName = false;
      renderStudent();
    }
  };
  $("change-name").onclick = () => {
    editingName = true;
    $("student-name").value = state.student.name;
    renderStudent();
    $("student-name").focus();
  };
  $("cancel-name").onclick = () => {
    editingName = false;
    renderStudent();
  };
}
refresh();
setInterval(refresh, 2000);
