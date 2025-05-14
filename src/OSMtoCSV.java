import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

public class OSMtoCSV {
    public static void main(String[] args) throws Exception {
        // Αρχείο εισόδου .osm και αρχείο εξόδου .csv
        File osmFile = new File("src/resources/kardia_map.osm");   // βεβαιώσου ότι είναι στο σωστό path
        File csvFile = new File("src/resources/data.csv");

        // Προετοιμασία parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(osmFile);
        doc.getDocumentElement().normalize();

        // Λίστα κόμβων <node>
        NodeList nodes = doc.getElementsByTagName("node");

        // Δημιουργία και εγγραφή στο CSV αρχείο
        BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
        writer.write("id,name,lat,lon\n");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);

            String id = node.getAttribute("id");
            String lat = node.getAttribute("lat");
            String lon = node.getAttribute("lon");

            // Ψάχνουμε για tag name, αν δεν βρούμε βάζουμε "unnamed location"
            NodeList tags = node.getElementsByTagName("tag");
            String name = " ";
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("name")) {
                    name = tag.getAttribute("v").replace(",", ""); // αφαιρούμε κόμματα για να μην χαλάει το CSV
                    break;
                }
            }

            writer.write(String.format("%s,%s,%s,%s\n", id, name, lat, lon));
        }

        writer.close();
        System.out.println("✅ CSV file created: data.csv");
    }
}
