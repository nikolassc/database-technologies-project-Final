import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
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


        RStarTree tree = null;

        if(insertRecordsFromDataFile) {
            System.out.println("Choose the R*-Tree construction method:");
            System.out.println("1. Incrementally insert records into the R*-Tree (one by one)");
            System.out.println("2. Bulk Load all records into the R*-Tree (faster initial build)");

            int choice = -1;
            while (choice != 1 && choice != 2) {
                System.out.print("Enter your choice (1 or 2): ");
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter 1 or 2.");
                }
            }

            boolean useBulkLoad = (choice != 1);

            System.out.println("Building R*Tree index from datafile...");
            System.out.println();
            startTime = System.nanoTime();
            tree = new RStarTree(useBulkLoad);
            endTime = System.nanoTime();
            duration_in_ms = (endTime - startTime);
            System.out.println();
            System.out.println("R*Tree index built in " +duration_in_ms / 1000000.0 + "ms");
        }
        ArrayList<Integer> dataMetaData = FilesHandler.getDataMetaData();
        ArrayList<Integer> indexMetaData = FilesHandler.getIndexMetaData();
        if(!insertRecordsFromDataFile){
            tree = new RStarTree(indexMetaData);
        }

        System.out.println("Datafile Metadata: [Dimensions: " + dataMetaData.getFirst() +
                ", Block Size: " + dataMetaData.get(1) +
                ", Total Blocks in File: " + dataMetaData.get(2)+"]");
        System.out.println("Index Metadata: [Dimensions: " + indexMetaData.getFirst() +
                ", Block Size: " + indexMetaData.get(1) +
                ", Total Blocks in File: " + indexMetaData.get(2)+
                ", Total Levels in Tree: " + indexMetaData.get(3)+"]");
        System.out.println();

        String selection;
        do {
            System.out.println("Please select an operation you would like to execute: \n" +
                    "0) Exit application \n" +
                    "1) Linear Range Query \n" +
                    "2) Range Query using R* Tree index\n" +
                    "3) Linear K-Nearest Neighbors Query\n" +
                    "4) K-Nearest Neighbors Query using R* Tree Index\n"+
                    "5) Linear Skyline Query\n" +
                    "6) Skyline Query using R* Tree Index\n"+
                    "7) Single Record insert\n" +
                    "8) Single Record delete\n");
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

                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Linear Range query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();
                    System.out.flush();


                    break;

                //      RANGE QUERY
                case "2":
                    System.out.println("Range Query using R* Tree index Selected");
                    boundsList = new ArrayList<>();
                    dims = FilesHandler.getDataDimensions();


                    System.out.println("Give Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Give bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();
                            scanner.nextLine();

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
                    queryResults = RangeQuery.rangeQuery(FilesHandler.readNode(RStarTree.getRootNodeBlockId(), 0), queryMBR);
                    endTime = System.nanoTime();
                    duration_in_ms = (endTime - startTime) / 1000000.0;


                    System.out.println("Results:");
                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Range Query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());
                    System.out.println();

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
                        scanner.nextLine();
                        queryPoint.add(val);
                    }

                    // Run k-NN query
                    startTime = System.nanoTime();
                    LinearNearestNeighboursQuery query = new LinearNearestNeighboursQuery(queryPoint, k);
                    queryResults = query.getNearestRecords();
                    endTime = System.nanoTime();

                    double duration = (endTime - startTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Linear K-Nearest Neighbors query completed in " + duration + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();
                    scanner.nextLine();
                    break;


                //      KNN QUERY
                case "4":
                    System.out.println("K-Nearest Neighbors Query using R* Tree Index Selected");
                    System.out.println("🔎 Executing Nearest Neighbours Query...");
                    System.out.print("Enter value for K: ");
                    int k2 = scanner.nextInt();
                    scanner.nextLine(); // <-- Απαραίτητο!
                    int dimensions2 = FilesHandler.getDataDimensions();
                    ArrayList<Double> queryPoint2 = new ArrayList<>();
                    System.out.println("Enter coordinates of the query point (you have " + dimensions2 + " dimensions):");
                    for (int i = 0; i < dimensions2; i++) {
                        System.out.print("Dimension " + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine(); // <-- Απαραίτητο σε κάθε αριθμό!
                        queryPoint2.add(val);
                    }
                    // Run k-NN query
                    startTime = System.nanoTime();
                    queryResults = NearestNeighboursQuery.getNearestNeighbours(queryPoint2, k2);
                    endTime = System.nanoTime();

                    double duration2 = (endTime - startTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    ;
                    System.out.println("K-Nearest Neighbors query completed in " + duration2 + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();   // Καθαρό newline


                    break;

                //      LINEAR SKYLINE QUERY
                case "5":
                    System.out.println("Linear Skyline Query Selected");

                    startTime = System.nanoTime();
                    queryResults = LinearSkylineQuery.computeSkyline();
                    endTime = System.nanoTime();

                    duration_in_ms = (endTime - startTime) / 1_000_000.0;

                    System.out.println("Records in skyline:");

                    for (Record r : queryResults) {
                        System.out.println(r.toString());
                    }
                    System.out.println("Linear Skyline Query completed in " + duration_in_ms + " ms");
                    System.out.println("Total skyline points: " + queryResults.size());
                    System.out.println();

                    break;

                //      SKYLINE QUERY
                case "6":

                    System.out.println("Skyline Query using R* Tree Index (Optimal) Selected");

                    startTime = System.nanoTime();

                    ArrayList<Record> skylineResults = OptimalSkylineQuery.computeSkyline();

                     endTime = System.nanoTime();
                    double durationInMs = (endTime - startTime) / 1_000_000.0;

                    System.out.println("Skyline Records:");

                    for (Record r : skylineResults) {
                        System.out.println(r);
                    }
                    System.out.println("Skyline Query completed in " + durationInMs + " ms");
                    System.out.println("Total skyline points: " + skylineResults.size());

                    System.out.println();
                    break;


                //      SINGLE RECORD INSERT
                case "7":
                    System.out.println("Single Record Insert Selected");
                    dims = FilesHandler.getDataDimensions();
                    ArrayList<Double> newCoords = new ArrayList<>();
                    System.out.println("Enter coordinates for the new record (you have " + dims + " dimensions):");

                    for (int i = 0; i < dims; i++) {
                        System.out.print("Dimension " + (i + 1) + ": ");
                        double val = 0;
                        try {
                            val = scanner.nextDouble();
                            scanner.nextLine();
                        } catch (Exception e) {
                            System.out.println("Invalid input for dimension " + (i + 1) + ". Please try again.");
                            scanner.nextLine(); // Clear invalid input
                            i--; // Retry this dimension
                            continue;
                        }
                        newCoords.add(val);
                    }

                    System.out.print("Enter Record ID (must be a unique Long): ");
                    long recordID = 0;
                    try {
                        recordID = scanner.nextLong();
                        scanner.nextLine();
                    } catch (Exception e) {
                        System.out.println("Invalid Record ID. Insert aborted.");
                        scanner.nextLine();
                        break;
                    }

                    System.out.print("Enter Record Name: ");
                    String recordName = scanner.nextLine().trim();
                    if (recordName.isEmpty()) {
                        System.out.println("Record name cannot be empty. Insert aborted.");
                        break;
                    }

                    Record newRecord = new Record(recordID, recordName,newCoords);

                    try {
                        tree.insertSingleRecord(newRecord);
                    } catch (Exception e) {
                        System.out.println("Error inserting record: " + e.getMessage());
                    }

                    System.out.println();
                    break;

                // SINGLE RECORD DELETE
                case "8":
                    System.out.println("Single Record Delete Selected");
                    System.out.print("Enter the Record ID to delete: ");
                    long deleteRecordID = 0;
                    try {
                        deleteRecordID = scanner.nextLong();
                        scanner.nextLine();
                    } catch (Exception e) {
                        System.out.println("Invalid Record ID. Delete aborted.");
                        scanner.nextLine();
                        break;
                    }

                    try {
                        assert tree != null;
                        tree.deleteRecord(deleteRecordID);
                    } catch (Exception e) {
                        System.out.println("Error deleting record: " + e.getMessage());
                    }

                    System.out.println();
                    break;

                //      OTHER VALUES
                default:
                    System.out.println("Please select a valid query.");

            }

        } while (!selection.equals("0"));

    }
}