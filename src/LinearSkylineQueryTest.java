import java.util.ArrayList;

public class LinearSkylineQueryTest {

    public static void main(String[] args) {
        FilesHandler.initializeDataFile(2, false);
        System.out.println("🔎 Executing Linear Skyline Query...");

        long start = System.nanoTime();
        ArrayList<Record> skyline = LinearSkylineQuery.computeSkyline();
        long end = System.nanoTime();

        double durationMs = (end - start) / 1_000_000.0;

        System.out.println("✅ Skyline Query completed in " + durationMs + " ms");
        System.out.println("📦 Total skyline points: " + skyline.size());
        System.out.println("🌍 Records:");

        for (Record r : skyline) {
            System.out.println(" - ID: " + r.getRecordID() +
                    ", Name: " + r.getName() +
                    ", Coords: " + r.getCoordinates());
        }
    }
}
