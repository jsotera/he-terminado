package es.jsotera.heterminado.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Datos del aula. Los cambios se realizan a través del servicio. */
public final class Classroom {
    private final int rows;
    private final int columns;
    private final List<Activity> activities;
    private final List<Student> students;
    private final boolean inverted;
    private final Set<Integer> aisles;

    public Classroom(
            int rows,
            int columns,
            List<Activity> activities,
            List<Student> students,
            boolean inverted,
            Set<Integer> aisles) {
        this.rows = rows;
        this.columns = columns;
        this.activities = List.copyOf(activities);
        this.students = List.copyOf(students);
        this.inverted = inverted;
        this.aisles = Set.copyOf(aisles);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public List<Student> getStudents() {
        return students;
    }

    public boolean isInverted() {
        return inverted;
    }

    public Set<Integer> getAisles() {
        return aisles;
    }

    public static Classroom empty() {
        return new Classroom(4, 5, List.of(), List.of(), false, Set.of());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Classroom value = (Classroom) other;
        return rows == value.rows
                && columns == value.columns
                && Objects.equals(activities, value.activities)
                && Objects.equals(students, value.students)
                && inverted == value.inverted
                && Objects.equals(aisles, value.aisles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, columns, activities, students, inverted, aisles);
    }
}
