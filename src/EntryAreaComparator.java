import java.util.Comparator;

public class EntryAreaComparator implements Comparator<Entry> {

    @Override
    public int compare(Entry e1, Entry e2) {
        return Double.compare(
                e1.getBoundingBox().getArea(),
                e2.getBoundingBox().getArea()
        );
    }
}
