package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import es.jsotera.heterminado.model.Classroom;
import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.repository.XmlRepository;
import es.jsotera.heterminado.service.ClassroomService;
import es.jsotera.heterminado.web.TeacherAccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
        ClassroomService service = service();
        service.addActivity("Bucles <y> arrays & objetos");
        Student student = service.register(null, "María & José");
        String activity = service.snapshot().getActivities().get(0).getId();
        service.complete(student.getId(), activity, true);
        service.move(student.getId(), 7);
        service.resize(5, 6);
        ClassroomService reloaded = service();
        assertEquals(service.snapshot(), reloaded.snapshot());
        assertEquals(8, reloaded.student(student.getId()).getSeat());
        assertEquals(Set.of(activity), reloaded.student(student.getId()).getCompleted());
    }

    @Test
    void renameKeepsIdentityAndProgress() {
        ClassroomService service = service();
        service.addActivity("Uno");
        Student student = service.register(null, "Ana");
        service.complete(student.getId(), service.snapshot().getActivities().get(0).getId(), true);
        Student renamed = service.register(student.getId(), "Ana García");
        assertEquals(student.getId(), renamed.getId());
        assertEquals(1, service.snapshot().getStudents().size());
        assertEquals(1, renamed.getCompleted().size());
    }

    @Test
    void moveSwapsOccupiedSeatsAndResizeUnseatsOutsideStudents() {
        ClassroomService service = service();
        Student a = service.register(null, "A");
        Student b = service.register(null, "B");
        service.move(a.getId(), 0);
        service.move(b.getId(), 19);
        service.move(a.getId(), 19);
        assertEquals(0, service.student(b.getId()).getSeat());
        assertEquals(19, service.student(a.getId()).getSeat());
        service.resize(2, 2);
        assertNull(service.student(a.getId()).getSeat());
        assertEquals(0, service.student(b.getId()).getSeat());
    }

    @Test
    void deletionAndResetKeepValidProgress() {
        ClassroomService service = service();
        service.addActivity("Uno");
        service.addActivity("Dos");
        Student student = service.register(null, "A");
        String first = service.snapshot().getActivities().get(0).getId(),
                second = service.snapshot().getActivities().get(1).getId();
        service.complete(student.getId(), first, true);
        service.complete(student.getId(), second, true);
        service.deleteActivity(first);
        assertEquals(Set.of(second), service.student(student.getId()).getCompleted());
        service.reset();
        assertTrue(service.student(student.getId()).getCompleted().isEmpty());
        assertEquals(1, service.snapshot().getActivities().size());
    }

    @Test
    void invalidInputDoesNotAlterState() {
        ClassroomService service = service();
        Classroom original = service.snapshot();
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.resize(0, 5);
                    }
                });
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.register(null, " ");
                    }
                });
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.addActivity("x".repeat(101));
                    }
                });
        assertEquals(original, service.snapshot());
    }

    @Test
    void corruptXmlIsNeverOverwritten() throws Exception {
        Path file = temp.resolve("aula.xml");
        Files.writeString(file, "<broken>");
        assertThrows(
                IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service();
                    }
                });
        assertEquals("<broken>", Files.readString(file));
    }

    @Test
    void failedSaveDoesNotPublishChanges() throws Exception {
        Path blocker = temp.resolve("blocked");
        Files.writeString(blocker, "file");
        ClassroomService service =
                new ClassroomService(new XmlRepository(blocker.resolve("aula.xml")));
        assertThrows(
                IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.addActivity("No guardada");
                    }
                });
        assertTrue(service.snapshot().getActivities().isEmpty());
    }

    @Test
    void invertedViewDoesNotChangeSeatsAndSurvivesOtherEdits() {
        ClassroomService service = service();
        Student student = service.register(null, "Ana");
        service.move(student.getId(), 7);
        service.setInverted(true);
        service.setAisle(2, true);
        service.addActivity("Uno");
        String activity = service.snapshot().getActivities().get(0).getId();
        service.complete(student.getId(), activity, true);
        service.register(student.getId(), "Ana García");
        service.deleteActivity(activity);
        service.reset();
        assertEquals(7, service.student(student.getId()).getSeat());
        assertTrue(service.snapshot().isInverted());
        assertEquals(Set.of(2), service.snapshot().getAisles());
        assertEquals(service.snapshot(), service().snapshot());
        service.setInverted(false);
        assertEquals(7, service.student(student.getId()).getSeat());
    }

    @Test
    void aisleUnseatsOccupantAndRejectsStudentUntilRemoved() {
        ClassroomService service = service();
        Student student = service.register(null, "Ana");
        service.move(student.getId(), 0);
        service.setAisle(0, true);
        assertNull(service.student(student.getId()).getSeat());
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.move(student.getId(), 0);
                    }
                });
        assertNull(service.student(student.getId()).getSeat());
        service.setAisle(0, false);
        service.move(student.getId(), 0);
        assertEquals(0, service.student(student.getId()).getSeat());
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.setAisle(-1, true);
                    }
                });
        assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.setAisle(20, true);
                    }
                });
    }

    @Test
    void resizePreservesAisleCoordinatesAndRemovesThoseOutside() {
        ClassroomService service = service();
        service.setAisle(7, true);
        service.setAisle(19, true);
        service.resize(4, 6);
        assertEquals(Set.of(8, 22), service.snapshot().getAisles());
        service.resize(2, 3);
        assertEquals(Set.of(5), service.snapshot().getAisles());
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
        ClassroomService service = service();
        assertFalse(service.snapshot().isInverted());
        assertTrue(service.snapshot().getAisles().isEmpty());
        assertEquals(0, service.student("ana").getSeat());
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
        assertThrows(
                IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service();
                    }
                });
        Files.writeString(
                file,
                """
                <classroom rows="2" columns="3"><aisles><aisle seat="6"/></aisles></classroom>
                """);
        assertThrows(
                IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service();
                    }
                });
    }

    @Test
    void teacherRequiresLocalConnection() {
        assertTrue(TeacherAccess.sameMachine("127.0.0.1", "127.0.0.1"));
        assertTrue(TeacherAccess.sameMachine("::1", "::1"));
        assertTrue(TeacherAccess.sameMachine("192.168.1.10", "192.168.1.10"));
        assertFalse(TeacherAccess.sameMachine("192.168.1.20", "192.168.1.10"));
    }
}
