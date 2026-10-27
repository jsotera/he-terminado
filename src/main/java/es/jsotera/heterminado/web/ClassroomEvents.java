package es.jsotera.heterminado.web;

import io.javalin.http.sse.SseClient;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Una conexión abierta: enviamos avisos solo cuando cambia el aula, sin latidos. */
public class ClassroomEvents implements Consumer<SseClient>, Runnable {
    private final Set<SseClient> clients = ConcurrentHashMap.newKeySet();

    @Override
    public void accept(final SseClient client) {
        client.keepAlive();
        client.onClose(
                new Runnable() {
                    @Override
                    public void run() {
                        clients.remove(client);
                    }
                });
        clients.add(client);
        sendChange(client);
    }

    private void sendChange(SseClient client) {
        synchronized (client) {
            client.sendEvent("change", "refresh");
        }
    }

    @Override
    public void run() {
        for (SseClient client : clients) {
            sendChange(client);
        }
    }
}
