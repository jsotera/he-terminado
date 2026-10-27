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
import java.util.concurrent.CopyOnWriteArrayList;

/** Métodos sincronizados para que dos conexiones no pierdan cambios entre sí. */
public class ClassroomService {
    private final XmlRepository repository;
    private Classroom state;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public ClassroomService(XmlRepository repository) {
        this.repository = repository;
        state = repository.load();
    }

    public synchronized Classroom snapshot() {
        return state;
    }

    public synchronized Student student(String id) {
        for (Student student : state.getStudents()) {
            if (student.getId().equals(id)) {
                return student;
            }
        }
        return null;
    }

    private void commit(Classroom next) {
        // Solo cambiamos el estado en memoria cuando el XML se ha guardado bien.
        repository.save(next);
        state = next;
        for (Runnable listener : changeListeners) {
            listener.run();
        }
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
            next =
                    new Student(
                            previous.getId(),
                            name,
                            previous.getCompleted(),
                            previous.getSeat(),
                            previous.isHidden());
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            if (!student.getId().equals(next.getId())) {
                students.add(student);
            }
        }
        students.add(next);
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
        return next;
    }

    public synchronized void addActivity(String title) {
        List<Activity> activities = new ArrayList<>(state.getActivities());
        Activity activity = new Activity(UUID.randomUUID().toString(), text(title, 100));
        activities.add(activity);
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        activities,
                        state.getStudents(),
                        state.isInverted(),
                        state.getAisles()));
    }

    private void requireActivity(String id) {
        for (Activity activity : state.getActivities()) {
            if (activity.getId().equals(id)) {
                return;
            }
        }
        throw new IllegalArgumentException("La actividad ya no existe.");
    }

    public synchronized void deleteActivity(String id) {
        requireActivity(id);
        List<Activity> activities = new ArrayList<>();
        for (Activity activity : state.getActivities()) {
            if (!activity.getId().equals(id)) {
                activities.add(activity);
            }
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            Set<String> done = new HashSet<>(student.getCompleted());
            done.remove(id);
            students.add(
                    new Student(
                            student.getId(),
                            student.getName(),
                            done,
                            student.getSeat(),
                            student.isHidden()));
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        activities,
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }

    public synchronized void complete(String studentId, String activityId, boolean completed) {
        requireActivity(activityId);
        if (student(studentId) == null) {
            throw new IllegalArgumentException("Introduce tu nombre de nuevo.");
        }
        if (student(studentId).isHidden()) {
            throw new IllegalArgumentException("El profesorado ha desactivado tu participación.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            if (student.getId().equals(studentId)) {
                Set<String> done = new HashSet<>(student.getCompleted());
                if (completed) {
                    done.add(activityId);
                } else {
                    done.remove(activityId);
                }
                students.add(
                        new Student(
                                student.getId(),
                                student.getName(),
                                done,
                                student.getSeat(),
                                student.isHidden()));
            } else {
                students.add(student);
            }
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }

    public synchronized void resize(int rows, int columns) {
        if (rows < 1 || rows > 12 || columns < 1 || columns > 12) {
            throw new IllegalArgumentException("Usa entre 1 y 12 filas y columnas.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            Integer seat = student.getSeat();
            if (seat != null) {
                // Conservamos la fila y columna al cambiar el tamaño del aula.
                int row = seat / state.getColumns();
                int column = seat % state.getColumns();
                if (row < rows && column < columns) {
                    seat = row * columns + column;
                } else {
                    seat = null;
                }
            }
            students.add(
                    new Student(
                            student.getId(),
                            student.getName(),
                            student.getCompleted(),
                            seat,
                            student.isHidden()));
        }
        Set<Integer> aisles = new HashSet<>();
        for (Integer seat : state.getAisles()) {
            int row = seat / state.getColumns();
            int column = seat % state.getColumns();
            if (row < rows && column < columns) {
                aisles.add(row * columns + column);
            }
        }
        commit(
                new Classroom(
                        rows,
                        columns,
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        aisles));
    }

    public synchronized void move(String id, Integer seat) {
        Student moving = student(id);
        if (moving == null) {
            throw new IllegalArgumentException("El alumno ya no existe.");
        }
        if (seat != null && (seat < 0 || seat >= state.getRows() * state.getColumns())) {
            throw new IllegalArgumentException("Asiento incorrecto.");
        }
        if (seat != null && state.getAisles().contains(seat)) {
            throw new IllegalArgumentException("No se puede colocar alumnado en un pasillo.");
        }

        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            Integer target = student.getSeat();
            if (student.getId().equals(id)) {
                target = seat;
            } else if (seat != null && seat.equals(student.getSeat())) {
                // Si el asiento está ocupado, intercambiamos los sitios.
                target = moving.getSeat();
            }
            students.add(
                    new Student(
                            student.getId(),
                            student.getName(),
                            student.getCompleted(),
                            target,
                            student.isHidden()));
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }

    public synchronized void reset() {
        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            students.add(
                    new Student(
                            student.getId(),
                            student.getName(),
                            Set.of(),
                            student.getSeat(),
                            student.isHidden()));
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }

    public synchronized void setInverted(boolean inverted) {
        // Cambia solo la representación: los identificadores de asiento se conservan.
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        state.getStudents(),
                        inverted,
                        state.getAisles()));
    }

    public synchronized void setAisle(int seat, boolean aisle) {
        if (seat < 0 || seat >= state.getRows() * state.getColumns()) {
            throw new IllegalArgumentException("Posición incorrecta.");
        }
        Set<Integer> aisles = new HashSet<>(state.getAisles());
        if (aisle) {
            aisles.add(seat);
        } else {
            aisles.remove(seat);
        }
        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            // Un pupitre convertido en pasillo deja al alumno en «Sin asiento».
            if (aisle && Integer.valueOf(seat).equals(student.getSeat())) {
                students.add(
                        new Student(
                                student.getId(),
                                student.getName(),
                                student.getCompleted(),
                                null,
                                student.isHidden()));
            } else {
                students.add(student);
            }
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        aisles));
    }

    public synchronized void deleteStudent(String id) {
        if (student(id) == null) throw new IllegalArgumentException("El alumno ya no existe.");
        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            if (!student.getId().equals(id)) students.add(student);
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }

    public synchronized void setStudentHidden(String id, boolean hidden) {
        if (student(id) == null) throw new IllegalArgumentException("El alumno ya no existe.");
        List<Student> students = new ArrayList<>();
        for (Student student : state.getStudents()) {
            if (student.getId().equals(id)) {
                students.add(
                        new Student(
                                student.getId(),
                                student.getName(),
                                student.getCompleted(),
                                student.getSeat(),
                                hidden));
            } else students.add(student);
        }
        commit(
                new Classroom(
                        state.getRows(),
                        state.getColumns(),
                        state.getActivities(),
                        students,
                        state.isInverted(),
                        state.getAisles()));
    }
}
