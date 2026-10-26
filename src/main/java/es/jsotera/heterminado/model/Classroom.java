package es.jsotera.heterminado.model;

import java.util.List;
import java.util.Set;

public record Classroom(
        int rows,
        int columns,
        List<Activity> activities,
        List<Student> students,
        boolean inverted,
        Set<Integer> aisles) {
    public Classroom {
        activities = List.copyOf(activities);
        students = List.copyOf(students);
        aisles = Set.copyOf(aisles);
    }

    public static Classroom empty() {
        return new Classroom(4, 5, List.of(), List.of(), false, Set.of());
    }
}
