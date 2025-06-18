import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;


/**
 * Public class {@link OSMtoCSV} that converts an OSM file to csv format.
 */


public class OSMtoCSV {
    public static void main(String[] args) throws Exception {
        File osmFile = new File("src/resources/kardia_map.osm");
        File csvFile = new File("src/resources/data.csv");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(osmFile);
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("node");

        BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
        writer.write("id,name,lat,lon\n");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);

            String id = node.getAttribute("id");
            String lat = node.getAttribute("lat");
            String lon = node.getAttribute("lon");

            NodeList tags = node.getElementsByTagName("tag");
            String name = " ";
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("name")) {
                    name = tag.getAttribute("v").replace(",", "default_name");
                    break;
                }
            }

            writer.write(String.format("%s,%s,%s,%s\n", id, name, lat, lon));
        }

        writer.close();
        System.out.println("CSV file created: data.csv");
    }
}
