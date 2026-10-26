package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
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
        Javalin app = Main.create(temp.resolve("aula.xml")).start("127.0.0.1", 0);
        try {
            HttpClient client = HttpClient.newHttpClient();
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
                HttpResponse<String> response =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + path)).build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), path);
                assertEquals(
                        "no-store",
                        response.headers().firstValue("Cache-Control").orElse(""),
                        path);
            }
            HttpResponse<String> response =
                    client.send(
                            HttpRequest.newBuilder(URI.create(base + "/manifest.webmanifest"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertTrue(
                    response.headers()
                            .firstValue("Content-Type")
                            .orElse("")
                            .contains("application/manifest+json"));
            JsonNode manifest = new ObjectMapper().readTree(response.body());
            assertEquals("/index", manifest.get("start_url").asText());
            assertEquals("standalone", manifest.get("display").asText());
            for (JsonNode icon : manifest.get("icons")) {
                HttpResponse<byte[]> png =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + icon.get("src").asText()))
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, png.statusCode());
                assertEquals("no-store", png.headers().firstValue("Cache-Control").orElse(""));
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png.body()));
                assertNotNull(image);
                assertEquals(
                        icon.get("sizes").asText(), image.getWidth() + "x" + image.getHeight());
            }
        } finally {
            app.stop();
        }
    }
}
