package es.jsotera.heterminado;

import es.jsotera.heterminado.repository.XmlRepository;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.web.ClassroomController;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinFreemarker;
import java.nio.file.Path;

public class App {
    public static Javalin create(Path data) {
        var controller = new ClassroomController(new ClassroomService(new XmlRepository(data)));
        var templates = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_34);
        templates.setClassForTemplateLoading(App.class, "/");
        templates.setDefaultEncoding("UTF-8");
        return Javalin.create(config -> {
            config.fileRenderer(new JavalinFreemarker(templates));
            config.staticFiles.add("/public");
            config.http.maxRequestSize = 16_384L;
            controller.routes(config);
        });
    }
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("port", "7070"));
        var app = create(Path.of(System.getProperty("data", "data/aula.xml"))).start("0.0.0.0", port);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        System.out.println("He terminado! · Profesorado: http://localhost:" + port + "/index");
        System.out.println("Alumnado: http://<IP-del-profesor>:" + port + "/index");
    }
}
