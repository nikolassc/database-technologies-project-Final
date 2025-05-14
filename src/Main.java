import java.io.*;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Δημιουργία εγγραφών
            ArrayList<Double> cafeCoords = new ArrayList<>();
            ArrayList<Double> museumCoords = new ArrayList<>();
            cafeCoords.add(37.9838);
            cafeCoords.add(23.7275);
            museumCoords.add(37.9681);
            museumCoords.add(23.7286);
            Record cafe = new Record(1, "Cafe Central", cafeCoords);
            Record museum = new Record(2, "Acropolis Museum", museumCoords);

            // 2. Δημιουργία block και προσθήκη εγγραφών
            Block block = new Block(0);
            block.addRecord(cafe);
            block.addRecord(museum);

            // 3. Μετατροπή σε bytes
            byte[] bytes = block.toBytes();
            System.out.println("Serialized block to bytes: " + bytes.length + " bytes");

            // 4. Ανάγνωση από bytes
            Block loadedBlock = Block.fromBytes(bytes);

            // 5. Εκτύπωση αποτελεσμάτων
            System.out.println("Deserialized Block:");
            System.out.println("Block ID: " + loadedBlock.getBlockID());
            System.out.println("Number of records: " + loadedBlock.recordCount());

            for (int i = 0; i < loadedBlock.recordCount(); i++) {
                System.out.println("Record " + i + ": " + loadedBlock.getRecord(i));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
