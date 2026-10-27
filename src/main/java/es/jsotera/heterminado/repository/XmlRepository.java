package es.jsotera.heterminado.repository;

import es.jsotera.heterminado.model.*;

import org.w3c.dom.*;

import java.nio.file.*;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** Un único archivo legible. No sobrescribimos un XML que no podamos leer. */
public class XmlRepository {
    private final Path file;

    public XmlRepository(Path file) {
        this.file = file.toAbsolutePath();
    }

    private DocumentBuilderFactory factory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    public Classroom load() {
        if (!Files.exists(file)) return Classroom.empty();
        try {
            Element root = factory().newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            if (!root.getTagName().equals("classroom"))
                throw new IllegalArgumentException("Raíz XML incorrecta");
            int rows = Integer.parseInt(root.getAttribute("rows"));
            int columns = Integer.parseInt(root.getAttribute("columns"));
            if (rows < 1 || rows > 12 || columns < 1 || columns > 12)
                throw new IllegalArgumentException("Dimensiones incorrectas");
            String invertedValue = root.getAttribute("inverted");
            if (!invertedValue.isEmpty()
                    && !invertedValue.equals("true")
                    && !invertedValue.equals("false"))
                throw new IllegalArgumentException("Orientación incorrecta");
            boolean inverted = Boolean.parseBoolean(invertedValue);
            Set<Integer> aisles = new HashSet<>();
            NodeList aisleNodes = root.getElementsByTagName("aisle");
            for (int i = 0; i < aisleNodes.getLength(); i++) {
                int seat = Integer.parseInt(((Element) aisleNodes.item(i)).getAttribute("seat"));
                if (seat < 0 || seat >= rows * columns || !aisles.add(seat))
                    throw new IllegalArgumentException("Pasillo incorrecto");
            }
            List<Activity> activities = new ArrayList<>();
            List<Student> students = new ArrayList<>();
            Set<String> activityIds = new HashSet<>(), studentIds = new HashSet<>();
            Set<Integer> seats = new HashSet<>();
            NodeList nodes = root.getElementsByTagName("activity");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                if (e.getAttribute("id").isBlank()
                        || !activityIds.add(e.getAttribute("id"))
                        || e.getTextContent().isBlank())
                    throw new IllegalArgumentException("Actividad incorrecta");
                activities.add(new Activity(e.getAttribute("id"), e.getTextContent()));
            }
            nodes = root.getElementsByTagName("student");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                if (e.getAttribute("id").isBlank()
                        || !studentIds.add(e.getAttribute("id"))
                        || e.getAttribute("name").isBlank())
                    throw new IllegalArgumentException("Alumno incorrecto");
                Integer seat =
                        e.hasAttribute("seat") ? Integer.valueOf(e.getAttribute("seat")) : null;
                if (seat != null
                        && (seat < 0
                                || seat >= rows * columns
                                || aisles.contains(seat)
                                || !seats.add(seat)))
                    throw new IllegalArgumentException("Asiento incorrecto");
                Set<String> completed = new HashSet<>();
                NodeList done = e.getElementsByTagName("done");
                for (int j = 0; j < done.getLength(); j++) {
                    String id = ((Element) done.item(j)).getAttribute("activityId");
                    if (!activityIds.contains(id))
                        throw new IllegalArgumentException("Actividad desconocida");
                    completed.add(id);
                }
                students.add(
                        new Student(
                                e.getAttribute("id"),
                                e.getAttribute("name"),
                                completed,
                                seat,
                                Boolean.parseBoolean(e.getAttribute("hidden"))));
            }
            return new Classroom(rows, columns, activities, students, inverted, aisles);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo leer " + file + ". Revisa el XML antes de arrancar.", e);
        }
    }

    public void save(Classroom state) {
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            Document doc = factory().newDocumentBuilder().newDocument();
            Element root = doc.createElement("classroom");
            doc.appendChild(root);
            root.setAttribute("rows", "" + state.getRows());
            root.setAttribute("columns", "" + state.getColumns());
            root.setAttribute("inverted", "" + state.isInverted());
            Element aisles = doc.createElement("aisles");
            root.appendChild(aisles);
            for (Integer seat : new TreeSet<>(state.getAisles())) {
                Element aisle = doc.createElement("aisle");
                aisle.setAttribute("seat", "" + seat);
                aisles.appendChild(aisle);
            }
            Element activities = doc.createElement("activities");
            root.appendChild(activities);
            for (Activity a : state.getActivities()) {
                Element e = doc.createElement("activity");
                e.setAttribute("id", a.getId());
                e.setTextContent(a.getTitle());
                activities.appendChild(e);
            }
            Element students = doc.createElement("students");
            root.appendChild(students);
            for (Student s : state.getStudents()) {
                Element e = doc.createElement("student");
                e.setAttribute("id", s.getId());
                e.setAttribute("name", s.getName());
                e.setAttribute("hidden", String.valueOf(s.isHidden()));
                if (s.getSeat() != null) e.setAttribute("seat", "" + s.getSeat());
                for (String id : s.getCompleted()) {
                    Element done = doc.createElement("done");
                    done.setAttribute("activityId", id);
                    e.appendChild(done);
                }
                students.appendChild(e);
            }
            TransformerFactory transformers = TransformerFactory.newInstance();
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformers.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            temp = Files.createTempFile(file.getParent(), "classroom-", ".tmp");
            transformer.transform(new DOMSource(doc), new StreamResult(temp.toFile()));
            try {
                Files.move(
                        temp,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar el aula", e);
        } finally {
            if (temp != null)
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
        }
    }
}
