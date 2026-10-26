package es.jsotera.heterminado.web.request;

/** Datos JSON enviados por el navegador. Jackson usa el constructor vacío y los setters. */
public class NameRequest {
    private String name;

    public NameRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
