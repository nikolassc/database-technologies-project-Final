import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean filesExist = Files.exists(Paths.get(FilesHandler.PATH_TO_DATAFILE));
        boolean resetFiles = false;

        Scanner scanner = new Scanner(System.in);

        if (filesExist) {
            System.out.println("Existed data-file and index-file found.");
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
            System.out.println();
        }

        FilesHandler.initializeDataFile(dataDimensions, resetFiles);
        FilesHandler.initializeIndexFile(dataDimensions, resetFiles);
    }
}