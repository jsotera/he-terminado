package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;

class HttpTest {
    @TempDir Path temp;

    @Test
    void servesTemplatesAssetsAndProtectsChanges() throws Exception {
        var app = Main.create(temp.resolve("aula.xml")).start("127.0.0.1", 0);
        try {
            var client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + app.port();
            var index =
                    client.send(
                            HttpRequest.newBuilder(URI.create(base + "/index")).build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(200, index.statusCode());
            assertTrue(index.body().contains("Profesorado"));
            assertTrue(
                    index.body().contains("Añadir actividad"),
                    "Las plantillas deben leerse como UTF-8 también en Windows");
            for (String asset : new String[] {"/app.js", "/style.css"}) {
                assertEquals(
                        200,
                        client.send(
                                        HttpRequest.newBuilder(URI.create(base + asset)).build(),
                                        HttpResponse.BodyHandlers.ofString())
                                .statusCode());
            }
            assertEquals(
                    400,
                    client.send(
                                    HttpRequest.newBuilder(URI.create(base + "/api/reset"))
                                            .POST(HttpRequest.BodyPublishers.ofString("{}"))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString())
                            .statusCode());
            assertEquals(
                    403,
                    client.send(
                                    HttpRequest.newBuilder(URI.create(base + "/api/reset"))
                                            .header("Content-Type", "application/json")
                                            .header("Origin", "http://other.example")
                                            .POST(HttpRequest.BodyPublishers.ofString("{}"))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString())
                            .statusCode());
            var added =
                    client.send(
                            HttpRequest.newBuilder(URI.create(base + "/api/activities"))
                                    .header("Content-Type", "application/json")
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"title\":\"Actividad de prueba\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(204, added.statusCode(), added.body());
            var state =
                    client.send(
                            HttpRequest.newBuilder(URI.create(base + "/api/state"))
                                    .header("X-Forwarded-For", "192.168.1.30")
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertTrue(state.body().contains("Actividad de prueba"));
            assertTrue(state.body().contains("teacher"));
        } finally {
            app.stop();
        }
    }
}
