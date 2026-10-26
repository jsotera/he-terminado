package es.jsotera.heterminado.web;

import es.jsotera.heterminado.config.Conf;
import es.jsotera.heterminado.model.Classroom;
import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.service.PresenceService;

import io.javalin.Javalin;
import io.javalin.http.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClassroomController {
    private static final String COOKIE = "heterminado_student";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassroomController.class);
    private final ClassroomService service;
    private final PresenceService presence = new PresenceService();

    public ClassroomController(ClassroomService service) {
        this.service = service;
    }

    // Cada record representa los datos JSON que envía un formulario del navegador.
    public record NameRequest(String name) {}

    public record ActivityRequest(String title) {}

    public record ProgressRequest(Boolean completed) {}

    public record LayoutRequest(int rows, int columns) {}

    public record SeatRequest(Integer seat) {}

    public record ViewRequest(Boolean inverted) {}

    public record AisleRequest(Boolean aisle) {}

    /** Igual que en HolaJavalin: cada ruta apunta a un método que recibe Context. */
    public void routes(Javalin app) {
        app.before(this::checkRequest);
        app.get("/", ctx -> ctx.redirect("/index"));
        app.get("/index", this::index);
        app.get("/api/state", this::state);
        app.post("/api/student", this::registerStudent);
        app.put("/api/progress/{id}", this::updateProgress);
        app.post("/api/activities", this::addActivity);
        app.delete("/api/activities/{id}", this::deleteActivity);
        app.put("/api/layout", this::resizeClassroom);
        app.put("/api/view", this::updateView);
        app.put("/api/aisles/{seat}", this::updateAisle);
        app.put("/api/seats/{id}", this::moveStudent);
        app.post("/api/reset", this::resetProgress);

        app.exception(IllegalArgumentException.class, this::invalidData);
        app.exception(HttpResponseException.class, this::httpError);
        app.exception(Exception.class, this::unexpectedError);
    }

    private void index(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("teacher", TeacherAccess.isTeacher(ctx));
        model.put("refreshInterval", Conf.INTERVALO_ACTUALIZACION_MS);
        ctx.header("Cache-Control", "no-store");
        ctx.render("/templates/index.ftl", model);
    }

    private void state(Context ctx) {
        Classroom classroom = service.snapshot();
        Map<String, Object> result = new HashMap<>();
        if (TeacherAccess.isTeacher(ctx)) {
            result.put("role", "teacher");
            result.put("classroom", classroom);
            result.put("onlineStudentIds", presence.onlineStudentIds(classroom.students()));
        } else {
            Student student = service.student(ctx.cookie(COOKIE));
            if (student != null) {
                // La consulta periódica del alumno funciona también como latido.
                presence.markSeen(student.id());
            }
            result.put("role", "student");
            result.put("activities", classroom.activities());
            result.put("student", student);
        }
        ctx.json(result);
    }

    private void registerStudent(Context ctx) {
        if (TeacherAccess.isTeacher(ctx)) {
            throw new ForbiddenResponse("Estás en la vista del profesorado.");
        }
        NameRequest request = ctx.bodyAsClass(NameRequest.class);
        Student student = service.register(ctx.cookie(COOKIE), request.name());
        presence.markSeen(student.id());
        ctx.header(
                "Set-Cookie",
                COOKIE
                        + "="
                        + student.id()
                        + "; Path=/; Max-Age=31536000; HttpOnly; SameSite=Strict");
        ctx.json(student);
    }

    private void updateProgress(Context ctx) {
        if (TeacherAccess.isTeacher(ctx)) {
            throw new ForbiddenResponse("El progreso lo indica cada alumno.");
        }
        String id = studentId(ctx);
        ProgressRequest request = ctx.bodyAsClass(ProgressRequest.class);
        if (request.completed() == null) {
            throw new BadRequestResponse("Falta el estado.");
        }
        service.complete(id, ctx.pathParam("id"), request.completed());
        presence.markSeen(id);
        ctx.status(204);
    }

    private void addActivity(Context ctx) {
        teacher(ctx);
        ActivityRequest request = ctx.bodyAsClass(ActivityRequest.class);
        service.addActivity(request.title());
        ctx.status(204);
    }

    private void deleteActivity(Context ctx) {
        teacher(ctx);
        service.deleteActivity(ctx.pathParam("id"));
        ctx.status(204);
    }

    private void resizeClassroom(Context ctx) {
        teacher(ctx);
        LayoutRequest request = ctx.bodyAsClass(LayoutRequest.class);
        service.resize(request.rows(), request.columns());
        ctx.status(204);
    }

    private void moveStudent(Context ctx) {
        teacher(ctx);
        SeatRequest request = ctx.bodyAsClass(SeatRequest.class);
        service.move(ctx.pathParam("id"), request.seat());
        ctx.status(204);
    }

    private void updateView(Context ctx) {
        teacher(ctx);
        ViewRequest request = ctx.bodyAsClass(ViewRequest.class);
        if (request.inverted() == null) {
            throw new BadRequestResponse("Falta la orientación del aula.");
        }
        service.setInverted(request.inverted());
        ctx.status(204);
    }

    private void updateAisle(Context ctx) {
        teacher(ctx);
        AisleRequest request = ctx.bodyAsClass(AisleRequest.class);
        if (request.aisle() == null) {
            throw new BadRequestResponse("Falta el estado del pasillo.");
        }
        service.setAisle(Integer.parseInt(ctx.pathParam("seat")), request.aisle());
        ctx.status(204);
    }

    private void resetProgress(Context ctx) {
        teacher(ctx);
        service.reset();
        ctx.status(204);
    }

    private void teacher(Context ctx) {
        if (!TeacherAccess.isTeacher(ctx)) {
            throw new ForbiddenResponse("Solo el profesorado puede hacer esto.");
        }
    }

    private String studentId(Context ctx) {
        String id = ctx.cookie(COOKIE);
        if (service.student(id) == null) {
            throw new UnauthorizedResponse("Introduce tu nombre para continuar.");
        }
        return id;
    }

    private void checkRequest(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; "
                        + "frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        if (!ctx.path().startsWith("/api/")) {
            return;
        }
        ctx.header("Cache-Control", "no-store");
        if (ctx.method().name().equals("GET")) {
            return;
        }

        // Exigimos JSON para impedir cambios enviados por formularios de otras webs.
        String type = ctx.contentType();
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            throw new BadRequestResponse("Se requiere JSON.");
        }
        String origin = ctx.header("Origin");
        String serverOrigin = ctx.scheme() + "://" + ctx.header("Host");
        if (origin != null && !origin.equals(serverOrigin)) {
            throw new ForbiddenResponse("Origen no permitido.");
        }
    }

    private void invalidData(IllegalArgumentException exception, Context ctx) {
        String message = exception.getMessage();
        if (message == null) {
            message = "Datos incorrectos";
        }
        ctx.status(400).json(Map.of("message", message));
    }

    private void httpError(HttpResponseException exception, Context ctx) {
        String message = exception.getMessage();
        if (message == null) {
            message = "Petición incorrecta";
        }
        ctx.status(exception.getStatus()).json(Map.of("message", message));
    }

    private void unexpectedError(Exception exception, Context ctx) {
        LOGGER.error("Error procesando petición", exception);
        ctx.status(500)
                .json(
                        Map.of(
                                "message",
                                "No se pudo completar la operación. Revisa la consola del"
                                        + " servidor."));
    }
}
