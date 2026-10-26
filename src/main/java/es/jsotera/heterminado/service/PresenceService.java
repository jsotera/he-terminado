package es.jsotera.heterminado.service;

import es.jsotera.heterminado.config.Conf;
import es.jsotera.heterminado.model.Student;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** La presencia es temporal: los latidos no escriben en el XML. */
public class PresenceService {
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long timeoutMillis;

    public PresenceService() {
        this(Clock.systemUTC(), Conf.TIEMPO_DESCONEXION_MS);
    }

    public PresenceService(Clock clock, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("El tiempo de desconexión debe ser positivo.");
        }
        this.clock = clock;
        this.timeoutMillis = timeoutMillis;
    }

    public void markSeen(String studentId) {
        lastSeen.put(studentId, clock.millis());
    }

    public Set<String> onlineStudentIds(List<Student> students) {
        long now = clock.millis();
        Set<String> online = new HashSet<>();
        for (Student student : students) {
            Long last = lastSeen.get(student.getId());
            if (last != null && now - last < timeoutMillis) {
                online.add(student.getId());
            }
        }
        return online;
    }
}
