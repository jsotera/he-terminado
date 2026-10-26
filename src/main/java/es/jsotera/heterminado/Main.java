package es.jsotera.heterminado;

import es.jsotera.heterminado.config.WebConfig;
import es.jsotera.heterminado.repository.XmlRepository;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.web.ClassroomController;

import io.javalin.Javalin;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("port", "7070"));
        Path data = Path.of(System.getProperty("data", "data/aula.xml"));

        Javalin app = create(data);
        app.start("0.0.0.0", port);

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        app.stop();
                                    }
                                }));
        System.out.println("He terminado! · Profesorado: http://localhost:" + port + "/index");
        System.out.println("Alumnado: http://<IP-del-profesor>:" + port + "/index");
    }

    /** Separamos la creación del arranque para poder probar el servidor en otro puerto. */
    public static Javalin create(Path data) {
        XmlRepository repository = new XmlRepository(data);
        ClassroomService service = new ClassroomService(repository);
        ClassroomController controller = new ClassroomController(service);

        Javalin app = Javalin.create(new WebConfig());

        controller.routes(app);
        return app;
    }
}
