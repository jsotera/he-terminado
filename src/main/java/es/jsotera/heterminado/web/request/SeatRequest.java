package es.jsotera.heterminado.web.request;

/** Datos JSON enviados por el navegador. Jackson usa el constructor vacío y los setters. */
public class SeatRequest {
    private Integer seat;

    public SeatRequest() {}

    public Integer getSeat() {
        return seat;
    }

    public void setSeat(Integer seat) {
        this.seat = seat;
    }
}
