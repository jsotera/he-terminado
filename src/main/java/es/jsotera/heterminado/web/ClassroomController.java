package es.jsotera.heterminado.web;

import es.jsotera.heterminado.service.ClassroomService;
import io.javalin.config.JavalinConfig;
import io.javalin.http.*;
import java.util.*;

public class ClassroomController {
    private final ClassroomService service;
    private static final String COOKIE = "heterminado_student";
    public ClassroomController(ClassroomService service) { this.service = service; }
    public record NameRequest(String name) { }
    public record ActivityRequest(String title) { }
    public record ProgressRequest(Boolean completed) { }
    public record LayoutRequest(int rows, int columns) { }
    public record SeatRequest(Integer seat) { }
    private void teacher(Context ctx) { if (!TeacherAccess.isTeacher(ctx)) throw new ForbiddenResponse("Solo el profesorado puede hacer esto."); }
    private String studentId(Context ctx) {
        String id = ctx.cookie(COOKIE);
        if (service.student(id) == null) throw new UnauthorizedResponse("Introduce tu nombre para continuar.");
        return id;
    }
    public void routes(JavalinConfig config) {
        config.routes.before(ctx -> {
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
            if (ctx.path().startsWith("/api/")) {
                ctx.header("Cache-Control", "no-store");
                // Exige JSON para impedir cambios enviados por formularios de otras webs.
                if (!ctx.method().name().equals("GET")) {
                    String type = ctx.contentType();
                    if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json")) throw new BadRequestResponse("Se requiere JSON.");
                    String origin = ctx.header("Origin");
                    if (origin != null && !origin.equals(ctx.scheme() + "://" + ctx.header("Host"))) throw new ForbiddenResponse("Origen no permitido.");
                }
            }
        });
        config.routes.get("/", ctx -> ctx.redirect("/index"));
        config.routes.get("/index", ctx -> { ctx.header("Cache-Control", "no-store"); ctx.render("templates/index.ftlh", Map.of("teacher", TeacherAccess.isTeacher(ctx))); });
        config.routes.get("/api/state", ctx -> {
            var state = service.snapshot();
            if (TeacherAccess.isTeacher(ctx)) ctx.json(Map.of("role", "teacher", "classroom", state));
            else {
                Map<String, Object> result = new HashMap<>(); result.put("role", "student");
                result.put("activities", state.activities()); result.put("student", service.student(ctx.cookie(COOKIE))); ctx.json(result);
            }
        });
        config.routes.post("/api/student", ctx -> {
            if (TeacherAccess.isTeacher(ctx)) throw new ForbiddenResponse("Estás en la vista del profesorado.");
            var student = service.register(ctx.cookie(COOKIE), ctx.bodyAsClass(NameRequest.class).name());
            ctx.header("Set-Cookie", COOKIE + "=" + student.id() + "; Path=/; Max-Age=31536000; HttpOnly; SameSite=Strict"); ctx.json(student);
        });
        config.routes.put("/api/progress/{id}", ctx -> {
            if (TeacherAccess.isTeacher(ctx)) throw new ForbiddenResponse("El progreso lo indica cada alumno.");
            String id = studentId(ctx); Boolean done = ctx.bodyAsClass(ProgressRequest.class).completed();
            if (done == null) throw new BadRequestResponse("Falta el estado.");
            service.complete(id, ctx.pathParam("id"), done); ctx.status(204);
        });
        config.routes.post("/api/activities", ctx -> { teacher(ctx); service.addActivity(ctx.bodyAsClass(ActivityRequest.class).title()); ctx.status(204); });
        config.routes.delete("/api/activities/{id}", ctx -> { teacher(ctx); service.deleteActivity(ctx.pathParam("id")); ctx.status(204); });
        config.routes.put("/api/layout", ctx -> { teacher(ctx); var body = ctx.bodyAsClass(LayoutRequest.class); service.resize(body.rows(), body.columns()); ctx.status(204); });
        config.routes.put("/api/seats/{id}", ctx -> { teacher(ctx); service.move(ctx.pathParam("id"), ctx.bodyAsClass(SeatRequest.class).seat()); ctx.status(204); });
        config.routes.post("/api/reset", ctx -> { teacher(ctx); service.reset(); ctx.status(204); });
        config.routes.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).json(Map.of("message", e.getMessage() == null ? "Datos incorrectos" : e.getMessage())));
        config.routes.exception(HttpResponseException.class, (e, ctx) -> ctx.status(e.getStatus()).json(Map.of("message", e.getMessage() == null ? "Petición incorrecta" : e.getMessage())));
        config.routes.exception(Exception.class, (e, ctx) -> { org.slf4j.LoggerFactory.getLogger(ClassroomController.class).error("Error procesando petición", e); ctx.status(500).json(Map.of("message", "No se pudo completar la operación. Revisa la consola del servidor.")); });
    }
}
