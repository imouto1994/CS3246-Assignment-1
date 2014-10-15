package FindIO;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImageResult implements  Comparable<ImageResult>{
    private static final String PATH_PROPERTY_ID = "path";
    private static final String RANK_PROPERTY_ID = "rank";

    private StringProperty path;
    private IntegerProperty rank;

    public ImageResult(String path, int rank) {
        checkProperty();
        setPath(path);
        setRank(rank);
    }

    public ImageResult() {
        checkProperty();
        initialize();
    }

    private void initialize() {
        setPath("");
        setRank(1);
    }

    private void checkProperty() {
        pathProperty();
        rankProperty();
    }

    public StringProperty pathProperty() {
        if(path == null) {
            path = new SimpleStringProperty(this, PATH_PROPERTY_ID);
        }
        return path;
    }

    public String getPath() {
        return this.path.get();
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public IntegerProperty rankProperty() {
        if(rank == null) {
            rank = new SimpleIntegerProperty(this, RANK_PROPERTY_ID);
        }
        return rank;
    }

    public int getRank() {
        return this.rank.get();
    }

    public void setRank(int rank) {
        this.rank.set(rank);
    }

    public String getImageName(){
        return null;
    }

    public int compareTo(ImageResult other) {
        return new Integer(this.getRank()).compareTo(new Integer(other.getRank()));
    }
}
