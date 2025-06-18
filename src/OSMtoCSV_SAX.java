import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Public class {@link OSMtoCSV_SAX} using an SAX parser for bigger files
 */


public class OSMtoCSV_SAX {
    public static void main(String[] args) {
        String inputFilePath = "src/resources/greece.osm";
        String outputFilePath = "src/resources/data.csv";

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write("id,name,lat,lon\n"); // header

            DefaultHandler handler = new DefaultHandler() {
                private String currentId;
                private String currentLat;
                private String currentLon;
                private String currentName = "";
                private boolean insideNode = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equals("node")) {
                        insideNode = true;
                        currentId = attributes.getValue("id");
                        currentLat = attributes.getValue("lat");
                        currentLon = attributes.getValue("lon");
                        currentName = "";
                    } else if (insideNode && qName.equals("tag")) {
                        String k = attributes.getValue("k");
                        if ("name".equals(k)) {
                            currentName = attributes.getValue("v").replace(",", "");
                        }
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equals("node") && currentId != null && currentLat != null && currentLon != null) {
                        try {
                            writer.write(currentId + "," + currentName + "," + currentLat + "," + currentLon + "\n");
                        } catch (IOException e) {
                            throw new SAXException(e);
                        }
                        insideNode = false;
                        currentId = null;
                    }
                }
            };

            saxParser.parse(new File(inputFilePath), handler);
            writer.close();
            System.out.println("CSV file created: " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
