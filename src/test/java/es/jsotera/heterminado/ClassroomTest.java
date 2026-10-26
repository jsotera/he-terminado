package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import es.jsotera.heterminado.repository.XmlRepository;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.web.TeacherAccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.Set;

class ClassroomTest {
    @TempDir Path temp;

    private ClassroomService service() {
        return new ClassroomService(new XmlRepository(temp.resolve("aula.xml")));
    }

    @Test
    void roundTripPreservesProgressNamesAndSeats() {
        var service = service();
        service.addActivity("Bucles <y> arrays & objetos");
        var student = service.register(null, "María & José");
        String activity = service.snapshot().activities().get(0).id();
        service.complete(student.id(), activity, true);
        service.move(student.id(), 7);
        service.resize(5, 6);
        var reloaded = service();
        assertEquals(service.snapshot(), reloaded.snapshot());
        assertEquals(8, reloaded.student(student.id()).seat());
        assertEquals(Set.of(activity), reloaded.student(student.id()).completed());
    }

    @Test
    void renameKeepsIdentityAndProgress() {
        var service = service();
        service.addActivity("Uno");
        var student = service.register(null, "Ana");
        service.complete(student.id(), service.snapshot().activities().get(0).id(), true);
        var renamed = service.register(student.id(), "Ana García");
        assertEquals(student.id(), renamed.id());
        assertEquals(1, service.snapshot().students().size());
        assertEquals(1, renamed.completed().size());
    }

    @Test
    void moveSwapsOccupiedSeatsAndResizeUnseatsOutsideStudents() {
        var service = service();
        var a = service.register(null, "A");
        var b = service.register(null, "B");
        service.move(a.id(), 0);
        service.move(b.id(), 19);
        service.move(a.id(), 19);
        assertEquals(0, service.student(b.id()).seat());
        assertEquals(19, service.student(a.id()).seat());
        service.resize(2, 2);
        assertNull(service.student(a.id()).seat());
        assertEquals(0, service.student(b.id()).seat());
    }

    @Test
    void deletionAndResetKeepValidProgress() {
        var service = service();
        service.addActivity("Uno");
        service.addActivity("Dos");
        var student = service.register(null, "A");
        String first = service.snapshot().activities().get(0).id(),
                second = service.snapshot().activities().get(1).id();
        service.complete(student.id(), first, true);
        service.complete(student.id(), second, true);
        service.deleteActivity(first);
        assertEquals(Set.of(second), service.student(student.id()).completed());
        service.reset();
        assertTrue(service.student(student.id()).completed().isEmpty());
        assertEquals(1, service.snapshot().activities().size());
    }

    @Test
    void invalidInputDoesNotAlterState() {
        var service = service();
        var original = service.snapshot();
        assertThrows(IllegalArgumentException.class, () -> service.resize(0, 5));
        assertThrows(IllegalArgumentException.class, () -> service.register(null, " "));
        assertThrows(IllegalArgumentException.class, () -> service.addActivity("x".repeat(101)));
        assertEquals(original, service.snapshot());
    }

    @Test
    void corruptXmlIsNeverOverwritten() throws Exception {
        Path file = temp.resolve("aula.xml");
        Files.writeString(file, "<broken>");
        assertThrows(IllegalStateException.class, this::service);
        assertEquals("<broken>", Files.readString(file));
    }

    @Test
    void failedSaveDoesNotPublishChanges() throws Exception {
        Path blocker = temp.resolve("blocked");
        Files.writeString(blocker, "file");
        var service = new ClassroomService(new XmlRepository(blocker.resolve("aula.xml")));
        assertThrows(IllegalStateException.class, () -> service.addActivity("No guardada"));
        assertTrue(service.snapshot().activities().isEmpty());
    }

    @Test
    void invertedViewDoesNotChangeSeatsAndSurvivesOtherEdits() {
        var service = service();
        var student = service.register(null, "Ana");
        service.move(student.id(), 7);
        service.setInverted(true);
        service.setAisle(2, true);
        service.addActivity("Uno");
        String activity = service.snapshot().activities().get(0).id();
        service.complete(student.id(), activity, true);
        service.register(student.id(), "Ana García");
        service.deleteActivity(activity);
        service.reset();
        assertEquals(7, service.student(student.id()).seat());
        assertTrue(service.snapshot().inverted());
        assertEquals(Set.of(2), service.snapshot().aisles());
        assertEquals(service.snapshot(), service().snapshot());
        service.setInverted(false);
        assertEquals(7, service.student(student.id()).seat());
    }

    @Test
    void aisleUnseatsOccupantAndRejectsStudentUntilRemoved() {
        var service = service();
        var student = service.register(null, "Ana");
        service.move(student.id(), 0);
        service.setAisle(0, true);
        assertNull(service.student(student.id()).seat());
        assertThrows(IllegalArgumentException.class, () -> service.move(student.id(), 0));
        assertNull(service.student(student.id()).seat());
        service.setAisle(0, false);
        service.move(student.id(), 0);
        assertEquals(0, service.student(student.id()).seat());
        assertThrows(IllegalArgumentException.class, () -> service.setAisle(-1, true));
        assertThrows(IllegalArgumentException.class, () -> service.setAisle(20, true));
    }

    @Test
    void resizePreservesAisleCoordinatesAndRemovesThoseOutside() {
        var service = service();
        service.setAisle(7, true);
        service.setAisle(19, true);
        service.resize(4, 6);
        assertEquals(Set.of(8, 22), service.snapshot().aisles());
        service.resize(2, 3);
        assertEquals(Set.of(5), service.snapshot().aisles());
        assertEquals(service.snapshot(), service().snapshot());
    }

    @Test
    void oldXmlLoadsWithNormalViewAndNoAisles() throws Exception {
        Files.writeString(
                temp.resolve("aula.xml"),
                """
                <classroom rows="2" columns="3">
                  <activities/>
                  <students><student id="ana" name="Ana" seat="0"/></students>
                </classroom>
                """);
        var service = service();
        assertFalse(service.snapshot().inverted());
        assertTrue(service.snapshot().aisles().isEmpty());
        assertEquals(0, service.student("ana").seat());
    }

    @Test
    void xmlRejectsStudentsInAislesAndOutOfRangeAisles() throws Exception {
        Path file = temp.resolve("aula.xml");
        Files.writeString(
                file,
                """
                <classroom rows="2" columns="3" inverted="true">
                  <aisles><aisle seat="0"/></aisles><activities/>
                  <students><student id="ana" name="Ana" seat="0"/></students>
                </classroom>
                """);
        assertThrows(IllegalStateException.class, this::service);
        Files.writeString(
                file,
                """
                <classroom rows="2" columns="3"><aisles><aisle seat="6"/></aisles></classroom>
                """);
        assertThrows(IllegalStateException.class, this::service);
    }

    @Test
    void teacherRequiresLocalConnection() {
        assertTrue(TeacherAccess.sameMachine("127.0.0.1", "127.0.0.1"));
        assertTrue(TeacherAccess.sameMachine("::1", "::1"));
        assertTrue(TeacherAccess.sameMachine("192.168.1.10", "192.168.1.10"));
        assertFalse(TeacherAccess.sameMachine("192.168.1.20", "192.168.1.10"));
    }
}
