package es.jsotera.heterminado.web;

import es.jsotera.heterminado.config.Conf;
import es.jsotera.heterminado.model.Classroom;
import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.service.PresenceService;
import es.jsotera.heterminado.web.request.*;

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

    /** Igual que en HolaJavalin: cada ruta apunta a un método que recibe Context. */
    public void routes(Javalin app) {
        app.before(
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        checkRequest(ctx);
                    }
                });
        // Las respuestas dinámicas no se guardan. Los estáticos se configuran en Main.
        app.after(
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        ctx.header("Cache-Control", "no-store");
                    }
                });
        app.get(
                "/",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        ctx.redirect("/index");
                    }
                });
        app.get(
                "/index",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        index(ctx);
                    }
                });
        app.get(
                "/api/state",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        state(ctx);
                    }
                });
        app.post(
                "/api/student",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        registerStudent(ctx);
                    }
                });
        app.put(
                "/api/progress/{id}",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        updateProgress(ctx);
                    }
                });
        app.post(
                "/api/activities",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        addActivity(ctx);
                    }
                });
        app.delete(
                "/api/activities/{id}",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        deleteActivity(ctx);
                    }
                });
        app.put(
                "/api/layout",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        resizeClassroom(ctx);
                    }
                });
        app.put(
                "/api/view",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        updateView(ctx);
                    }
                });
        app.put(
                "/api/aisles/{seat}",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        updateAisle(ctx);
                    }
                });
        app.put(
                "/api/seats/{id}",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        moveStudent(ctx);
                    }
                });
        app.post(
                "/api/reset",
                new Handler() {
                    @Override
                    public void handle(Context ctx) {
                        resetProgress(ctx);
                    }
                });

        app.exception(
                IllegalArgumentException.class,
                new ExceptionHandler<IllegalArgumentException>() {
                    @Override
                    public void handle(IllegalArgumentException exception, Context ctx) {
                        invalidData(exception, ctx);
                    }
                });
        app.exception(
                HttpResponseException.class,
                new ExceptionHandler<HttpResponseException>() {
                    @Override
                    public void handle(HttpResponseException exception, Context ctx) {
                        httpError(exception, ctx);
                    }
                });
        app.exception(
                Exception.class,
                new ExceptionHandler<Exception>() {
                    @Override
                    public void handle(Exception exception, Context ctx) {
                        unexpectedError(exception, ctx);
                    }
                });
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
            result.put("onlineStudentIds", presence.onlineStudentIds(classroom.getStudents()));
        } else {
            Student student = service.student(ctx.cookie(COOKIE));
            if (student != null) {
                // La consulta periódica del alumno funciona también como latido.
                presence.markSeen(student.getId());
            }
            result.put("role", "student");
            result.put("activities", classroom.getActivities());
            result.put("student", student);
        }
        ctx.json(result);
    }

    private void registerStudent(Context ctx) {
        if (TeacherAccess.isTeacher(ctx)) {
            throw new ForbiddenResponse("Estás en la vista del profesorado.");
        }
        NameRequest request = ctx.bodyAsClass(NameRequest.class);
        Student student = service.register(ctx.cookie(COOKIE), request.getName());
        presence.markSeen(student.getId());
        ctx.header(
                "Set-Cookie",
                COOKIE
                        + "="
                        + student.getId()
                        + "; Path=/; Max-Age=31536000; HttpOnly; SameSite=Strict"
                        + (ctx.req().isSecure() ? "; Secure" : ""));
        ctx.json(student);
    }

    private void updateProgress(Context ctx) {
        if (TeacherAccess.isTeacher(ctx)) {
            throw new ForbiddenResponse("El progreso lo indica cada alumno.");
        }
        String id = studentId(ctx);
        ProgressRequest request = ctx.bodyAsClass(ProgressRequest.class);
        if (request.getCompleted() == null) {
            throw new BadRequestResponse("Falta el estado.");
        }
        service.complete(id, ctx.pathParam("id"), request.getCompleted());
        presence.markSeen(id);
        ctx.status(204);
    }

    private void addActivity(Context ctx) {
        teacher(ctx);
        ActivityRequest request = ctx.bodyAsClass(ActivityRequest.class);
        service.addActivity(request.getTitle());
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
        service.resize(request.getRows(), request.getColumns());
        ctx.status(204);
    }

    private void moveStudent(Context ctx) {
        teacher(ctx);
        SeatRequest request = ctx.bodyAsClass(SeatRequest.class);
        service.move(ctx.pathParam("id"), request.getSeat());
        ctx.status(204);
    }

    private void updateView(Context ctx) {
        teacher(ctx);
        ViewRequest request = ctx.bodyAsClass(ViewRequest.class);
        if (request.getInverted() == null) {
            throw new BadRequestResponse("Falta la orientación del aula.");
        }
        service.setInverted(request.getInverted());
        ctx.status(204);
    }

    private void updateAisle(Context ctx) {
        teacher(ctx);
        AisleRequest request = ctx.bodyAsClass(AisleRequest.class);
        if (request.getAisle() == null) {
            throw new BadRequestResponse("Falta el estado del pasillo.");
        }
        service.setAisle(Integer.parseInt(ctx.pathParam("seat")), request.getAisle());
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
                        + "worker-src 'self'; manifest-src 'self'; "
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
