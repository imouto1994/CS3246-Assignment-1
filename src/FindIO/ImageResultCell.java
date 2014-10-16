package FindIO;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.controlsfx.control.GridCell;

import java.io.File;

public class ImageResultCell extends GridCell<ImageResult> {
    private RelevanceFeedbackInterface rf;
    private String imageID;

    // Constructor
    public ImageResultCell(double width, double height, RelevanceFeedbackInterface rf) {
        setWidth(width);
        setHeight(height);
        this.rf = rf;
    }

    public ImageResultCell() {
        this(0, 0, null);
    }

    @Override
    protected void updateItem(ImageResult item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null) {
            StackPane stack = new StackPane();
            ImageView imageView = new ImageView();
            File imageFile = new File(item.getPath());
            this.imageID = Common.removeExtension(imageFile.getName());
            Image defaultImage = new Image(imageFile.toURI().toString());
            if((defaultImage.getHeight() / defaultImage.getWidth()) >= 1.0){
                imageView.setFitHeight(this.getHeight());
                stack.setMaxHeight(this.getHeight());
                stack.setPrefHeight(this.getHeight());
                stack.setMaxWidth(defaultImage.getWidth() * this.getHeight() / defaultImage.getHeight());
                stack.setPrefWidth(defaultImage.getWidth() * this.getHeight() / defaultImage.getHeight());
            } else {
                imageView.setFitWidth(this.getWidth());
                stack.setMaxWidth(this.getWidth());
                stack.setPrefWidth(this.getWidth());
                stack.setMaxHeight(defaultImage.getHeight() * this.getWidth() / defaultImage.getWidth());
                stack.setPrefHeight(defaultImage.getHeight() * this.getWidth() / defaultImage.getWidth());
            }
            imageView.setPreserveRatio(true);
            imageView.setImage(defaultImage);

            Button upvoteButton = new Button("ACCEPT");
            upvoteButton.getStyleClass().add("greenButton");
            upvoteButton.setId("upvoteButton");
            upvoteButton.setPrefWidth(stack.getPrefWidth() / 2.0);
            upvoteButton.setPrefHeight(40.0);
            upvoteButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    rf.upvote(imageID);
                }
            });

            Button downvoteButton = new Button("REJECT");
            downvoteButton.getStyleClass().add("defaultButton");
            downvoteButton.setId("downvoteButton");
            downvoteButton.setPrefWidth(stack.getPrefWidth() / 2.0);
            downvoteButton.setPrefHeight(40.0);
            downvoteButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    rf.downvote(imageID);
                }
            });

            final HBox hBox = new HBox();
            hBox.setAlignment(Pos.BOTTOM_CENTER);
            hBox.setSpacing(0.0);
            hBox.getChildren().addAll(upvoteButton, downvoteButton);
            hBox.setTranslateY(40.0);

            stack.getChildren().addAll(imageView, hBox);
            stack.setClip(new Rectangle(stack.getPrefWidth(), stack.getPrefHeight()));
            stack.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    TranslateTransition translateTransition = new TranslateTransition(Duration.millis(200), hBox);
                    translateTransition.setInterpolator(Interpolator.EASE_BOTH);
                    translateTransition.setToY(0);
                    translateTransition.play();
                }
            });

            stack.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    TranslateTransition translateTransition = new TranslateTransition(Duration.millis(200), hBox);
                    translateTransition.setInterpolator(Interpolator.EASE_BOTH);
                    translateTransition.setToY(40);
                    translateTransition.play();
                }
            });

            StackPane.setAlignment(imageView, Pos.CENTER);
            StackPane.setAlignment(hBox, Pos.BOTTOM_CENTER);
            setGraphic(stack);
            setAlignment(Pos.CENTER);
        }
    }
}
