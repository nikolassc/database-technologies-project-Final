import java.util.ArrayList;
import java.io.Serializable;


/**
 * Public class {@link Record} refers to a single data record from the csv file that corresponds to geographical data
 * <p> Each {@link Record} has a {@code recordID}, a {@code name} and an {@link ArrayList} of {@link Double} as coordinates
 */

public class Record implements Serializable{
    private long recordID;
    private String name;
    private ArrayList<Double> coor; //coordinates


    /**
     * The {@link Record} constructor method that receives as parameters a {@code recordId}, {@code name}, and {@code coor} {@link ArrayList}
     * @param recordID {@link Long} the record's id
     * @param name {@link String} the record's name
     * @param coor {@link ArrayList<Double>} the record's coordinates
     */


    public Record(long recordID, String name, ArrayList<Double> coor){
        this.recordID = recordID;
        this.name = name;
        this.coor = coor;
    }


    /**
     * The {@link Record} constructor that receives a record in {@link String} format and parses it to {@link Record} format
     * @param recordInString A {@link Record} in {@link String} format
     */


    public Record(String recordInString) {
        String[] stringArray = recordInString.split(",");

        // Expecting: ID + name + coordinates
        if (stringArray.length != FilesHandler.getDataDimensions() + 2)
            throw new IllegalArgumentException("Record input string is not correct: " + recordInString);

        recordID = Long.parseLong(stringArray[0]);
        name = stringArray[1];

        coor = new ArrayList<>();
        for (int i = 2; i < stringArray.length; i++) {
            coor.add(Double.parseDouble(stringArray[i]));
        }
    }


    /**
     * Getter for the {@code recordId}
     * @return The record's id
     */


    public long getRecordID(){
        return  recordID;
    }


    /**
     * Getter for the {@code name}
     * @return The record's name
     */

    public String getName(){
        return  name;
    }


    /**
     * Getter for the {@code coordinates}
     * @return The record's coordinates
     */


    public ArrayList<Double> getCoordinates() { return coor;}


    /**
     * Getter for the given {@code dimension}'s coordinates
     * @param dimension The given dimension
     * @return The dimension's coordinates
     */


    public double getCoordinateFromDimension(int dimension){
        return  coor.get(dimension);
    }


    /**
     * Custom output of {@link Record} object to {@link String} format
     * @return The {@link Record} in {@link String} format
     */


    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder("ID: " + recordID + ", Name: " + name+ ", Coordinates: ");
        for (int i = 0;  i < coor.size(); i++) {
            if(i > 0)
                recordToString.append(", ");
            recordToString.append(coor.get(i));
        }
        return recordToString.toString();
    }
}
