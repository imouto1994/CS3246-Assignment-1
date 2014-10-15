package FindIO;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.controlsfx.control.GridCell;

import java.io.File;

public class ImageResultCell extends GridCell<ImageResult> {

    // Constructor
    public ImageResultCell(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    public ImageResultCell() {
        this(0, 0);
    }

    @Override
    protected void updateItem(ImageResult item, boolean empty) {
        super.updateItem(item, empty);
        ImageView imageView = new ImageView();
        File imageFile = new File(item.getPath());
        Image defaultImage = new Image(imageFile.toURI().toString());
        if((defaultImage.getHeight() / defaultImage.getWidth()) >= 1.0){
            imageView.setFitHeight(this.getHeight());
        } else {
            imageView.setFitWidth(this.getWidth());
        }
        imageView.setPreserveRatio(true);
        imageView.setImage(defaultImage);
        if (item != null) {
            setGraphic(imageView);
            setAlignment(Pos.CENTER);
        }
    }
}
