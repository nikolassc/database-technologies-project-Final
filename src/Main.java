import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Δημιουργία εγγραφών
            Records cafe = new Records("1", "Cafe Central", new double[]{37.9838, 23.7275});
            Records museum = new Records("2", "Acropolis Museum", new double[]{37.9681, 23.7286});

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
