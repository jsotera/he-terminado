package es.jsotera.heterminado.service;

import es.jsotera.heterminado.model.*;
import es.jsotera.heterminado.repository.XmlRepository;
import java.util.*;

/** Métodos sincronizados para que dos conexiones no pierdan cambios entre sí. */
public class ClassroomService {
    private final XmlRepository repository;
    private Classroom state;
    public ClassroomService(XmlRepository repository) { this.repository = repository; state = repository.load(); }
    public synchronized Classroom snapshot() { return state; }
    public synchronized Student student(String id) { return state.students().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null); }
    private void commit(Classroom next) { repository.save(next); state = next; }
    private String text(String text, int max) {
        if (text == null || text.isBlank() || text.strip().length() > max) throw new IllegalArgumentException("Escribe un texto entre 1 y " + max + " caracteres.");
        return text.strip();
    }
    public synchronized Student register(String id, String name) {
        name = text(name, 60);
        Student previous = student(id);
        Student next = new Student(previous == null ? UUID.randomUUID().toString() : previous.id(), name,
                previous == null ? Set.of() : previous.completed(), previous == null ? null : previous.seat());
        List<Student> students = new ArrayList<>(state.students());
        students.removeIf(s -> s.id().equals(next.id())); students.add(next);
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students)); return next;
    }
    public synchronized void addActivity(String title) {
        List<Activity> activities = new ArrayList<>(state.activities());
        activities.add(new Activity(UUID.randomUUID().toString(), text(title, 100)));
        commit(new Classroom(state.rows(), state.columns(), activities, state.students()));
    }
    private void requireActivity(String id) {
        if (state.activities().stream().noneMatch(a -> a.id().equals(id))) throw new IllegalArgumentException("La actividad ya no existe.");
    }
    public synchronized void deleteActivity(String id) {
        requireActivity(id);
        List<Activity> activities = state.activities().stream().filter(a -> !a.id().equals(id)).toList();
        List<Student> students = state.students().stream().map(s -> { var done = new HashSet<>(s.completed()); done.remove(id); return new Student(s.id(), s.name(), done, s.seat()); }).toList();
        commit(new Classroom(state.rows(), state.columns(), activities, students));
    }
    public synchronized void complete(String studentId, String activityId, boolean completed) {
        requireActivity(activityId);
        if (student(studentId) == null) throw new IllegalArgumentException("Introduce tu nombre de nuevo.");
        List<Student> students = state.students().stream().map(s -> {
            if (!s.id().equals(studentId)) return s;
            var done = new HashSet<>(s.completed()); if (completed) done.add(activityId); else done.remove(activityId);
            return new Student(s.id(), s.name(), done, s.seat());
        }).toList();
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
    }
    public synchronized void resize(int rows, int columns) {
        if (rows < 1 || rows > 12 || columns < 1 || columns > 12) throw new IllegalArgumentException("Usa entre 1 y 12 filas y columnas.");
        // Conservamos la fila y columna de cada alumno al cambiar el tamaño.
        List<Student> students = state.students().stream().map(s -> {
            Integer seat = s.seat();
            if (seat != null) { int row = seat / state.columns(), col = seat % state.columns(); seat = row < rows && col < columns ? row * columns + col : null; }
            return new Student(s.id(), s.name(), s.completed(), seat);
        }).toList();
        commit(new Classroom(rows, columns, state.activities(), students));
    }
    public synchronized void move(String id, Integer seat) {
        Student moving = student(id);
        if (moving == null) throw new IllegalArgumentException("El alumno ya no existe.");
        if (seat != null && (seat < 0 || seat >= state.rows() * state.columns())) throw new IllegalArgumentException("Asiento incorrecto.");
        List<Student> students = state.students().stream().map(s -> {
            Integer target = s.seat();
            if (s.id().equals(id)) target = seat;
            else if (seat != null && seat.equals(s.seat())) target = moving.seat();
            return new Student(s.id(), s.name(), s.completed(), target);
        }).toList();
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
    }
    public synchronized void reset() {
        commit(new Classroom(state.rows(), state.columns(), state.activities(), state.students().stream().map(s -> new Student(s.id(), s.name(), Set.of(), s.seat())).toList()));
    }
}
