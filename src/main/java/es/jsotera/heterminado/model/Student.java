package es.jsotera.heterminado.model;

import java.util.Set;

public record Student(String id, String name, Set<String> completed, Integer seat) {
    public Student { completed = Set.copyOf(completed); }
}
