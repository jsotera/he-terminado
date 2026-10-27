package es.jsotera.heterminado;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.jsotera.heterminado.model.Activity;
import es.jsotera.heterminado.model.Classroom;
import es.jsotera.heterminado.model.Student;
import es.jsotera.heterminado.web.request.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class JsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsAllRequestClassesUsingTheirSetters() throws Exception {
        NameRequest name = mapper.readValue("{\"name\":\"Ana\"}", NameRequest.class);
        ActivityRequest activity =
                mapper.readValue("{\"title\":\"Bucles\"}", ActivityRequest.class);
        ProgressRequest progress = mapper.readValue("{\"completed\":true}", ProgressRequest.class);
        LayoutRequest layout = mapper.readValue("{\"rows\":3,\"columns\":4}", LayoutRequest.class);
        SeatRequest seat = mapper.readValue("{\"seat\":5}", SeatRequest.class);
        ViewRequest view = mapper.readValue("{\"inverted\":false}", ViewRequest.class);
        AisleRequest aisle = mapper.readValue("{\"aisle\":true}", AisleRequest.class);
        assertEquals("Ana", name.getName());
        assertEquals("Bucles", activity.getTitle());
        assertEquals(Boolean.TRUE, progress.getCompleted());
        assertEquals(3, layout.getRows());
        assertEquals(4, layout.getColumns());
        assertEquals(5, seat.getSeat());
        assertEquals(Boolean.FALSE, view.getInverted());
        assertEquals(Boolean.TRUE, aisle.getAisle());
        assertNull(mapper.readValue("{}", ProgressRequest.class).getCompleted());
        assertNull(mapper.readValue("{\"seat\":null}", SeatRequest.class).getSeat());
    }

    @Test
    void gettersKeepTheJsonFormatUsedByTheBrowser() throws Exception {
        Activity activity = new Activity("a", "Bucles");
        Student student = new Student("ana", "Ana", Set.of("a"), 0);
        Classroom classroom =
                new Classroom(2, 3, List.of(activity), List.of(student), true, Set.of(2));
        JsonNode actual = mapper.valueToTree(classroom);
        JsonNode expected =
                mapper.readTree(
                        """
      {
        "rows":2,"columns":3,"inverted":true,"aisles":[2],
        "activities":[{"id":"a","title":"Bucles"}],
"students":[{"id":"ana","name":"Ana","completed":["a"],"seat":0,"hidden":false}]
      }
""");
        assertEquals(expected, actual);
    }
}
