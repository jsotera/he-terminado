package es.jsotera.heterminado;

import es.jsotera.heterminado.config.HttpsConfig;
import es.jsotera.heterminado.repository.XmlRepository;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.web.ClassroomController;

import freemarker.template.Configuration;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinFreemarker;

import java.nio.file.Path;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("port", "7070"));
        Path data = Path.of(System.getProperty("data", "data/aula.xml"));

        Javalin app = create(data);
        app.start("0.0.0.0", port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        String protocol = HttpsConfig.enabled() ? "https" : "http";
        int accessPort = HttpsConfig.enabled() ? HttpsConfig.port() : port;
        System.out.println(
                "He terminado! · Profesorado: "
                        + protocol
                        + "://localhost:"
                        + accessPort
                        + "/index");
        System.out.println(
                "Alumnado: " + protocol + "://<IP-del-profesor>:" + accessPort + "/index");
    }

    /** Separamos la creación del arranque para poder probar el servidor en otro puerto. */
    public static Javalin create(Path data) {
        XmlRepository repository = new XmlRepository(data);
        ClassroomService service = new ClassroomService(repository);
        ClassroomController controller = new ClassroomController(service);

        Configuration templates = new Configuration(Configuration.VERSION_2_3_32);
        templates.setClassForTemplateLoading(Main.class, "/");
        templates.setDefaultEncoding("UTF-8");

        Javalin app =
                Javalin.create(
                        config -> {
                            config.staticFiles.add(
                                    files -> {
                                        files.directory = "/public";
                                        files.headers = Map.of("Cache-Control", "no-store");
                                        files.mimeTypes.add(
                                                "application/manifest+json", "webmanifest");
                                    });
                            config.fileRenderer(new JavalinFreemarker(templates));
                            config.http.maxRequestSize = 16_384L;
                            HttpsConfig.configure(config);
                        });

        controller.routes(app);
        return app;
    }
}
