package es.jsotera.heterminado.model;

import java.util.Objects;
import java.util.Set;

/** Datos del aula. Los cambios se realizan a través del servicio. */
public final class Student {
    private final String id;
    private final String name;
    private final Set<String> completed;
    private final Integer seat;

    public Student(String id, String name, Set<String> completed, Integer seat) {
        this.id = id;
        this.name = name;
        this.completed = Set.copyOf(completed);
        this.seat = seat;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getCompleted() {
        return completed;
    }

    public Integer getSeat() {
        return seat;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Student value = (Student) other;
        return Objects.equals(id, value.id)
                && Objects.equals(name, value.name)
                && Objects.equals(completed, value.completed)
                && Objects.equals(seat, value.seat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, completed, seat);
    }
}
