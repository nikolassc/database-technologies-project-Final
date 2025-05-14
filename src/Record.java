import java.util.ArrayList;
import java.io.Serializable;

public class Record implements Serializable{
    private long recordID;
    private String name;
    private ArrayList<Double> coor; //coordinates

    // The constructor
    public Record(long recordID, String name, ArrayList<Double> coor){
        this.recordID = recordID;
        this.name = name;
        this.coor = coor;
    }
    public Record(String recordInString){
        String[] stringArray = recordInString.split(FilesHandler.getDelimiter());

        if(stringArray.length != FilesHandler.getDataDimensions() + 2)
            throw new IllegalArgumentException("Record input string is not correct");

        recordID = Long.parseLong(stringArray[0]);
        coor = new ArrayList<>();
        for(int i = 1; i < stringArray.length; i++){
            coor.add(Double.parseDouble(stringArray[i]));
        }
    }

    // Getters
    public long getRecordID(){
        return  recordID;
    }

    public String getName(){
        return  name;
    }

    public ArrayList<Double> getCoordinates(){
        return  coor;
    }

    // Custom output for the records
    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder(recordID + "," + coor.get(0));
        for(int i = 1; i < coor.size(); i++)
            recordToString.append(",").append(coor.get(i));
        return String.valueOf(recordToString);
    }
}
