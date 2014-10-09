package FindIO;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.controlsfx.control.GridCell;

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
        Rectangle rect = new Rectangle(this.getWidth(), this.getHeight());
        if (item != null) {
            rect.setFill(Color.RED);
            setGraphic(rect);
        }
    }
}
