package es.jsotera.heterminado.model;

import java.util.List;

public record Classroom(int rows, int columns, List<Activity> activities, List<Student> students) {
    public Classroom {
        activities = List.copyOf(activities);
        students = List.copyOf(students);
    }

    public static Classroom empty() {
        return new Classroom(4, 5, List.of(), List.of());
    }
}
