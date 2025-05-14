
public class Records {
    private String recordID;
    private String name;
    private double[] coor; //coordinates

    // The constructor
    public Records(String recordID, String name, double[] coor){
        this.recordID = recordID;
        this.name = name;
        this.coor = coor.clone();
    }

    // Setters
    public void setRecordID(String recordID){
        this.recordID = recordID;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setCoordinates(double[] coor){
        this.coor = coor.clone();
    }

    // Getters
    public String getRecordID(){
        return  recordID;
    }

    public String getName(){
        return  name;
    }

    public double[] getCoordinates(){
        return  coor.clone() ;
    }

    // Custom output for the records
    @Override
    public String toString() {
        return "Records{" +
                "id='" + recordID + '\'' +
                ", name='" + name + '\'' +
                ", coordinates=" + java.util.Arrays.toString(coor) +
                '}';
    }
}
