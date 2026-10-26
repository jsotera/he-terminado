package es.jsotera.heterminado.web.request;

/** Datos JSON enviados por el navegador. Jackson usa el constructor vacío y los setters. */
public class AisleRequest {
    private Boolean aisle;

    public AisleRequest() {}

    public Boolean getAisle() {
        return aisle;
    }

    public void setAisle(Boolean aisle) {
        this.aisle = aisle;
    }
}
