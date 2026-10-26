package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.service.PresenceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

class PresenceTest {
    private static class TestClock extends Clock {
        private long now;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(now);
        }

        @Override
        public long millis() {
            return now;
        }
    }

    private final Student ana = new Student("ana", "Ana", Set.of(), 0);
    private final Student luis = new Student("luis", "Luis", Set.of(), null);

    @Test
    void timesOutAtThirtySecondsAndReconnectsWithoutChangingStudent() {
        TestClock clock = new TestClock();
        PresenceService presence = new PresenceService(clock, 30_000);
        List<Student> students = List.of(ana, luis);
        assertTrue(presence.onlineStudentIds(students).isEmpty());
        presence.markSeen(ana.getId());
        clock.now = 29_999;
        assertEquals(Set.of("ana"), presence.onlineStudentIds(students));
        clock.now = 30_000;
        assertTrue(presence.onlineStudentIds(students).isEmpty());
        presence.markSeen(ana.getId());
        assertEquals(Set.of("ana"), presence.onlineStudentIds(students));
        assertEquals(0, ana.getSeat());
    }

    @Test
    void eachHeartbeatRenewsOnlyItsStudentAndTimeoutIsConfigurable() {
        TestClock clock = new TestClock();
        PresenceService presence = new PresenceService(clock, 5_000);
        presence.markSeen(ana.getId());
        presence.markSeen(luis.getId());
        clock.now = 4_000;
        presence.markSeen(ana.getId());
        clock.now = 5_000;
        assertEquals(Set.of("ana"), presence.onlineStudentIds(List.of(ana, luis)));
        clock.now = 9_000;
        assertTrue(presence.onlineStudentIds(List.of(ana, luis)).isEmpty());
    }

    @Test
    void restartRequiresNewHeartbeatAndUnknownIdsAreNotReported() {
        TestClock clock = new TestClock();
        PresenceService presence = new PresenceService(clock, 30_000);
        presence.markSeen("unknown");
        assertTrue(presence.onlineStudentIds(List.of(ana)).isEmpty());
        presence.markSeen(ana.getId());
        PresenceService restarted = new PresenceService(clock, 30_000);
        assertTrue(restarted.onlineStudentIds(List.of(ana)).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        new PresenceService(clock, 0);
                    }
                });
    }
}
