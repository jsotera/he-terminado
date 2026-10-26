package es.jsotera.heterminado.model;

import java.util.Objects;

/** Datos del aula. Los cambios se realizan a través del servicio. */
public final class Activity {
    private final String id;
    private final String title;

    public Activity(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Activity value = (Activity) other;
        return Objects.equals(id, value.id) && Objects.equals(title, value.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
}
