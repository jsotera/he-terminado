package es.jsotera.heterminado.web;

import io.javalin.http.Context;

import java.net.InetAddress;

public class TeacherAccess {
    /** Usamos la conexión TCP real; no confiamos en cabeceras X-Forwarded-For. */
    public static boolean isTeacher(Context ctx) {
        return sameMachine(ctx.req().getRemoteAddr(), ctx.req().getLocalAddr());
    }

    public static boolean sameMachine(String remote, String local) {
        try {
            InetAddress client = InetAddress.getByName(remote),
                    server = InetAddress.getByName(local);
            return client.isLoopbackAddress() || client.equals(server);
        } catch (Exception e) {
            return false;
        }
    }
}
