package es.jsotera.heterminado.service;

import es.jsotera.heterminado.model.Activity;
import es.jsotera.heterminado.model.Classroom;
import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.repository.XmlRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Métodos sincronizados para que dos conexiones no pierdan cambios entre sí. */
public class ClassroomService {
    private final XmlRepository repository;
    private Classroom state;

    public ClassroomService(XmlRepository repository) {
        this.repository = repository;
        state = repository.load();
    }

    public synchronized Classroom snapshot() {
        return state;
    }

    public synchronized Student student(String id) {
        for (Student student : state.students()) {
            if (student.id().equals(id)) {
                return student;
            }
        }
        return null;
    }

    private void commit(Classroom next) {
        // Solo cambiamos el estado en memoria cuando el XML se ha guardado bien.
        repository.save(next);
        state = next;
    }

    private String text(String text, int max) {
        if (text == null || text.isBlank() || text.strip().length() > max) {
            throw new IllegalArgumentException(
                    "Escribe un texto entre 1 y " + max + " caracteres.");
        }
        return text.strip();
    }

    public synchronized Student register(String id, String name) {
        name = text(name, 60);
        Student previous = student(id);
        Student next;
        if (previous == null) {
            next = new Student(UUID.randomUUID().toString(), name, Set.of(), null);
        } else {
            next = new Student(previous.id(), name, previous.completed(), previous.seat());
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            if (!student.id().equals(next.id())) {
                students.add(student);
            }
        }
        students.add(next);
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
        return next;
    }

    public synchronized void addActivity(String title) {
        List<Activity> activities = new ArrayList<>(state.activities());
        Activity activity = new Activity(UUID.randomUUID().toString(), text(title, 100));
        activities.add(activity);
        commit(new Classroom(state.rows(), state.columns(), activities, state.students()));
    }

    private void requireActivity(String id) {
        for (Activity activity : state.activities()) {
            if (activity.id().equals(id)) {
                return;
            }
        }
        throw new IllegalArgumentException("La actividad ya no existe.");
    }

    public synchronized void deleteActivity(String id) {
        requireActivity(id);
        List<Activity> activities = new ArrayList<>();
        for (Activity activity : state.activities()) {
            if (!activity.id().equals(id)) {
                activities.add(activity);
            }
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            Set<String> done = new HashSet<>(student.completed());
            done.remove(id);
            students.add(new Student(student.id(), student.name(), done, student.seat()));
        }
        commit(new Classroom(state.rows(), state.columns(), activities, students));
    }

    public synchronized void complete(String studentId, String activityId, boolean completed) {
        requireActivity(activityId);
        if (student(studentId) == null) {
            throw new IllegalArgumentException("Introduce tu nombre de nuevo.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            if (student.id().equals(studentId)) {
                Set<String> done = new HashSet<>(student.completed());
                if (completed) {
                    done.add(activityId);
                } else {
                    done.remove(activityId);
                }
                students.add(new Student(student.id(), student.name(), done, student.seat()));
            } else {
                students.add(student);
            }
        }
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
    }

    public synchronized void resize(int rows, int columns) {
        if (rows < 1 || rows > 12 || columns < 1 || columns > 12) {
            throw new IllegalArgumentException("Usa entre 1 y 12 filas y columnas.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            Integer seat = student.seat();
            if (seat != null) {
                // Conservamos la fila y columna al cambiar el tamaño del aula.
                int row = seat / state.columns();
                int column = seat % state.columns();
                if (row < rows && column < columns) {
                    seat = row * columns + column;
                } else {
                    seat = null;
                }
            }
            students.add(new Student(student.id(), student.name(), student.completed(), seat));
        }
        commit(new Classroom(rows, columns, state.activities(), students));
    }

    public synchronized void move(String id, Integer seat) {
        Student moving = student(id);
        if (moving == null) {
            throw new IllegalArgumentException("El alumno ya no existe.");
        }
        if (seat != null && (seat < 0 || seat >= state.rows() * state.columns())) {
            throw new IllegalArgumentException("Asiento incorrecto.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            Integer target = student.seat();
            if (student.id().equals(id)) {
                target = seat;
            } else if (seat != null && seat.equals(student.seat())) {
                // Si el asiento está ocupado, intercambiamos los sitios.
                target = moving.seat();
            }
            students.add(new Student(student.id(), student.name(), student.completed(), target));
        }
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
    }

    public synchronized void reset() {
        List<Student> students = new ArrayList<>();
        for (Student student : state.students()) {
            students.add(new Student(student.id(), student.name(), Set.of(), student.seat()));
        }
        commit(new Classroom(state.rows(), state.columns(), state.activities(), students));
    }
}
