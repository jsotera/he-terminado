package es.jsotera.heterminado.web.request;

/** Datos JSON enviados por el navegador. Jackson usa el constructor vacío y los setters. */
public class ProgressRequest {
    private Boolean completed;

    public ProgressRequest() {}

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
