import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean filesExist = Files.exists(Paths.get(FilesHandler.PATH_TO_DATAFILE));
        boolean resetFiles = false;

        Scanner scanner = new Scanner(System.in);

        if (filesExist) {
            System.out.println("Data-file and index-file already exist");
            System.out.print("Do you want to make new ones based on the data of the " + FilesHandler.getPathToCsv() +  " file? (y/n): ");
            String answer;
            while (true)
            {
                answer = scanner.nextLine().trim().toLowerCase();
                System.out.println();
                // In case user wants to reset the files
                if (answer.equals("y")) {
                    resetFiles = true;
                    break;
                } else if (answer.equals("n")) {
                    break;
                } else {
                    System.out.println("Please answer with y/n: ");
                }
            }
        }

        boolean insertRecordsFromDataFile = false;
        int dataDimensions = 0;

        if(!filesExist || resetFiles) {
            insertRecordsFromDataFile = true;
            System.out.print("Give the dimensions of the spacial data (dimensions need to be the same as the data saved in " + FilesHandler.getPathToCsv() + "): ");
            dataDimensions = scanner.nextInt();
            scanner.nextLine();
            System.out.println();
        }

        FilesHandler.initializeDataFile(dataDimensions, resetFiles);
        FilesHandler.initializeIndexFile(dataDimensions, resetFiles);

        double duration_in_ms;
        long startTime;
        long endTime;

        if(insertRecordsFromDataFile) {
            System.out.println("Building R*Tree index from datafile...");
            startTime = System.nanoTime();
            new RStarTree(false);
            endTime = System.nanoTime();
            duration_in_ms = (endTime - startTime);
            System.out.println("R*Tree index built in " +duration_in_ms / 1000000.0 + "ms");
        }
        System.out.println("Datafile Metadata: " + FilesHandler.getDataMetaData());
        System.out.println("Index Metadata: " + FilesHandler.getIndexMetaData());

        String selection;
        do {
            System.out.println("Please select a query you would like to execute: \n" +
                    "0) Exit application \n" +
                    "1) Linear Range Query \n" +
                    "2) Range Query using R* Tree index (WIP)\n" + //@TODO Range Query using Index
                    "3) Linear K-Nearest Neighbors Query\n" +
                    "4) K-Nearest Neighbors Query using R* Tree Index (WIP)\n"+ //@TODO K-nn Query using Index
                    "5) Linear Skyline Query\n" +
                    "6) Skyline Query using R* Tree Index (WIP)"); //@TODO Skyline Query using Index
            selection = scanner.nextLine().trim();
            System.out.println();

            // Initializing variables for queries
            ArrayList<Record> queryResults;
            ArrayList<Bounds> boundsList;
            int dims;
            MBR queryMBR;

            switch (selection) {

                //      EXIT
                case "0":
                    System.out.println("Exiting application");
                    break;

                //      LINEAR RANGE QUERY
                case "1":
                    System.out.println("Linear Range Query Selected");

                    //Runs for more than 2 dimensions if needed
                    boundsList = new ArrayList<>();
                    dims = FilesHandler.getDataDimensions();

                    System.out.println("Give Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Give bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                boundsList.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound. Try again.");
                            }
                        }
                    }

                    queryMBR = new MBR(boundsList);
                    startTime = System.nanoTime();
                    queryResults = LinearRangeQuery.runLinearRangeQuery(queryMBR);
                    endTime = System.nanoTime();
                    duration_in_ms = (endTime - startTime) / 1000000.0;

                    System.out.println("Linear Range query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }

                    System.out.println();


                    break;

                //      RANGE QUERY
                case "2":
                    System.out.println("Range Query using R* Tree index Selected(WIP)");
                    boundsList = new ArrayList<>();
                    dims = FilesHandler.getDataDimensions();


                    System.out.println("Give Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Give bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                boundsList.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound. Try again.");
                            }
                        }
                    }

                    queryMBR = new MBR(boundsList);
                    startTime = System.nanoTime();
                    queryResults = RangeQuery.rangeQuery(new Node(FilesHandler.getTotalLevelsFile()), queryMBR);
                    endTime = System.nanoTime();
                    duration_in_ms = (endTime - startTime) / 1000000.0;


                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Range Query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());
                    System.out.println("Results:");

                    break;

                //      LINEAR KNN QUERY
                case "3":
                    System.out.println("Linear K-Nearest Neighbors Query Selected");

                    System.out.print("Enter value for K: ");
                    int k = scanner.nextInt();
                    scanner.nextLine(); // <-- Απαραίτητο!

                    int dimensions = FilesHandler.getDataDimensions();
                    ArrayList<Double> queryPoint = new ArrayList<>();
                    System.out.println("Enter coordinates of the query point (you have " + dimensions + " dimensions):");
                    for (int i = 0; i < dimensions; i++) {
                        System.out.print("Dimension " + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine(); // <-- Απαραίτητο σε κάθε αριθμό!
                        queryPoint.add(val);
                    }

                    // Run k-NN query
                    startTime = System.nanoTime();
                    LinearNearestNeighboursQuery query = new LinearNearestNeighboursQuery(queryPoint, k);
                    queryResults = query.getNearestRecords();
                    endTime = System.nanoTime();

                    double duration = (endTime - startTime) / 1_000_000.0;
                    System.out.println("Linear K-Nearest Neighbors query completed in " + duration + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }

                    System.out.println();   // Καθαρό newline
                    System.out.flush();     // Πλήρες flush της κονσόλας
                    break;


                //      KNN QUERY
                case "4":
                    System.out.println("K-Nearest Neighbors Query using R* Tree Index Selected(WIP)");
                    break;

                //      LINEAR SKYLINE QUERY
                case "5":
                    System.out.println("Linear Skyline Query Selected");

                    startTime = System.nanoTime();
                    queryResults = LinearSkylineQuery.computeSkyline();
                    endTime = System.nanoTime();

                    duration_in_ms = (endTime - startTime) / 1_000_000.0;

                    System.out.println("Linear Skyline Query completed in " + duration_in_ms + " ms");
                    System.out.println("Total skyline points: " + queryResults.size());
                    System.out.println("Records in skyline:");

                    for (Record r : queryResults) {
                        System.out.println(r.toString());
                    }
                    System.out.println();

                    break;

                //      SKYLINE QUERY
                case "6":
                    System.out.println("Skyline Query using R* Tree Index Selected (WIP)");
                    break;

                //      OTHER VALUES
                default:
                    System.out.println("Please select a valid query.");

            }

        } while (!selection.equals("0"));

    }
}