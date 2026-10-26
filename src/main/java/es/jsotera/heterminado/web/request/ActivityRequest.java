package es.jsotera.heterminado.web.request;

/** Datos JSON enviados por el navegador. Jackson usa el constructor vacío y los setters. */
public class ActivityRequest {
    private String title;

    public ActivityRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
