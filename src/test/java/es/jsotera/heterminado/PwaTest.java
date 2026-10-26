package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import javax.imageio.ImageIO;

class PwaTest {
    @TempDir Path temp;

    @Test
    void pwaResourcesBypassHttpCacheAndIconsMatchManifest() throws Exception {
        var app = Main.create(temp.resolve("aula.xml")).start("127.0.0.1", 0);
        try {
            var client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + app.port();
            for (String path :
                    new String[] {
                        "/index",
                        "/app.js",
                        "/pwa.js",
                        "/style.css",
                        "/sw.js",
                        "/manifest.webmanifest",
                        "/api/state"
                    }) {
                var response =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + path)).build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), path);
                assertEquals(
                        "no-store",
                        response.headers().firstValue("Cache-Control").orElse(""),
                        path);
            }
            var response =
                    client.send(
                            HttpRequest.newBuilder(URI.create(base + "/manifest.webmanifest"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertTrue(
                    response.headers()
                            .firstValue("Content-Type")
                            .orElse("")
                            .contains("application/manifest+json"));
            var manifest = new ObjectMapper().readTree(response.body());
            assertEquals("/index", manifest.get("start_url").asText());
            assertEquals("standalone", manifest.get("display").asText());
            for (var icon : manifest.get("icons")) {
                var png =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + icon.get("src").asText()))
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, png.statusCode());
                assertEquals("no-store", png.headers().firstValue("Cache-Control").orElse(""));
                var image = ImageIO.read(new ByteArrayInputStream(png.body()));
                assertNotNull(image);
                assertEquals(
                        icon.get("sizes").asText(), image.getWidth() + "x" + image.getHeight());
            }
        } finally {
            app.stop();
        }
    }
}
