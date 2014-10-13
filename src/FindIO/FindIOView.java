package FindIO;

import java.awt.image.BufferedImage;
import java.io.File;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.imgscalr.Scalr;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class FindIOView {

    /* Pane Section */
	private Stage primaryStage;
	private Scene primaryScene;
	private Pane mainRoot;
    private Pane navBar;
    private Pane sideBar;
    private Pane centerSection;

    /* Screen Attributes */
	private double SCREEN_WIDTH  = Screen.getPrimary().getVisualBounds().getWidth();
	private double SCREEN_HEIGHT = Screen.getPrimary().getVisualBounds().getHeight();

    /* UI Controls */
	private FileChooser imageChooser;
    private TextField textField;
    private Button imageButton;
	private ImageView thumbNail;
	private Label imageNameLabel;
	private Label imageExtensionLabel;
	private Label imageSizeLabel;
    private Rectangle overlayLayer;
    private CheckBox checkBoxForHistogram;
    private CheckBox checkBoxForSIFT;
    private CheckBox checkBoxForConcept;

    /* Image Results List */
    ObservableList<ImageResult> imageList = FXCollections.observableArrayList();

    /* Constructor */
	public FindIOView(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

    /* UI Control Accessors */
    public FileChooser getImageChooser(){
        return imageChooser;
    }
    public TextField getTextField() { return textField; }

	/* Initialization Functions */
	public void initGUI() {
		mainRoot = initContent();
		initStage();
		initScene();
	}
	
	private void initStage() {
		primaryStage.setTitle("find.io");
		primaryStage.setMaximized(false);
		primaryStage.getIcons().add(new Image(getClass().getResource("./Images/logo.png").toExternalForm()));
	}
	
	private void initScene() {
		primaryScene = new Scene(mainRoot);
		primaryScene.getStylesheets().addAll(getClass().getResource("FindIOStyleSheet.css")
				.toExternalForm());
		primaryStage.setScene(primaryScene);
		primaryStage.show();
	}
	
	private Pane initContent() {
		navBar = initTopSection();
		centerSection = initCenterSection();
		BorderPane mainContent = new BorderPane();
		mainContent.setTop(navBar);
		mainContent.setCenter(centerSection);
		
		return mainContent;
	}
	
	private Pane initTopSection() {
		Pane top = new AnchorPane();
		top.setId("navBar");
		top.setPrefHeight(100.0);

		// Text Input
		textField = new TextField();
		textField.setPromptText("Search Text");
		textField.setPrefWidth(SCREEN_WIDTH / 2.0);
		textField.setPrefHeight(40.0);
		
		// Button Upload Image
		imageChooser = new FileChooser();
		configureImageChooser(imageChooser);
		imageButton = new Button("Image");
		imageButton.getStyleClass().add("transparentButton");
		imageButton.setId("imageButton");
		imageButton.setPrefHeight(40.0);
		ImageView imageGlyph = new ImageView();
		imageGlyph.setId("imageGlyph");
		imageGlyph.setFitHeight(25.0);
		imageGlyph.setPreserveRatio(true);
		imageGlyph.setSmooth(true);
		imageButton.setGraphic(imageGlyph);
		
		// Stack pane for upload image and text field
		StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.CENTER_RIGHT);
		stackPane.setPrefWidth(textField.getPrefWidth());
		stackPane.getChildren().addAll(textField, imageButton);
		
		// Button Search
		Button searchButton = new Button("Search");
		searchButton.getStyleClass().add("defaultButton");
		searchButton.setId("searchButton");
		searchButton.setPrefHeight(40.0);
		searchButton.setPrefWidth(75.0);
		ImageView searchGlyph = new ImageView();
		searchGlyph.setFitHeight(35.0);
		searchGlyph.setPreserveRatio(true);
		searchGlyph.setSmooth(true);
		searchGlyph.setImage(new Image(getClass().getResourceAsStream("./Images/searchGlyph.png")));
		searchButton.setGraphic(searchGlyph);
		linkCollapseSidePane(searchButton);

		// Logo
		ImageView logo = new ImageView();
		logo.setFitHeight(80.0);
		logo.setPreserveRatio(true);
		logo.setSmooth(true);
		logo.setImage(new Image(getClass().getResourceAsStream("./Images/logo_transparent.png")));
		
		HBox subBox = new HBox();
		subBox.setPrefHeight(top.getPrefHeight());
		subBox.setAlignment(Pos.CENTER);
		subBox.getChildren().addAll(stackPane, searchButton);
		
		HBox box = new HBox();
		box.setPrefHeight(top.getPrefHeight());
		box.getChildren().addAll(logo, subBox);
		box.setAlignment(Pos.CENTER);
		box.setSpacing(SCREEN_WIDTH / 40.0);
		
		top.getChildren().addAll(box);

		AnchorPane.setLeftAnchor(box, SCREEN_WIDTH / 60.0);
		
		return top;
	}

    private void linkCollapseSidePane(Button button) {
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                animateSideBar();
            }
        });
    }

    private void animateSideBar() {
        boolean shouldAnimation = false;
        if(sideBar.getTranslateX() == 0 || sideBar.getTranslateX() == -sideBar.getWidth()){
            shouldAnimation = true;
        }

        if(!shouldAnimation){
            return;
        }

        boolean shouldShow = false;
        if(sideBar.getTranslateX() != 0){
            shouldShow = true;
        }

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(400), sideBar);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), overlayLayer);
        if(shouldShow){
            centerSection.getChildren().add(1, overlayLayer);
            fadeTransition.setToValue(0.45);
            translateTransition.setByX(sideBar.getWidth());
        } else {
            fadeTransition.setToValue(0.0);
            translateTransition.setByX(-sideBar.getWidth());
        }
        fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
        fadeTransition.setAutoReverse(false);
        fadeTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(overlayLayer.getOpacity() == 0){
                    centerSection.getChildren().remove(overlayLayer);
                }
            }
        });
        translateTransition.setInterpolator(Interpolator.EASE_OUT);
        translateTransition.setAutoReverse(false);

        ParallelTransition finalTransition = new ParallelTransition();
        finalTransition.getChildren().addAll(fadeTransition, translateTransition);
        finalTransition.play();
    }
	
	public void linkImageChooser(final FindIOImageChooserInterface handler) {
		imageButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
					File file = imageChooser.showOpenDialog(primaryStage);
					
					if(file != null){
						String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
						String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
						Image queryImage = new Image(file.toURI().toString());
						int width = (int) queryImage.getWidth();
						int height = (int) queryImage.getHeight();
						queryImage = resizeImageWithSpecifiedHeight(queryImage, SCREEN_HEIGHT / 3.5);
						thumbNail.setImage(queryImage);
						imageNameLabel.setText(fileName.toUpperCase());
						imageExtensionLabel.setText(fileExtension.toUpperCase());
						imageSizeLabel.setText(width + " x " + height);
                        handler.imageSelectHandle(file);
					}
			}
		});
	}
	
	private void configureImageChooser(FileChooser imageChooser) {
		imageChooser.setTitle("Image Query");
		imageChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		imageChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("GIF", "*.gif")
        );
    }
	
	private Pane initCenterSection() {
		StackPane center = new StackPane();
        center.setId("centerSection");
        sideBar = initSideBar();
        overlayLayer = initOverlayLayer();
        GridView<ImageResult> grid = initGridView();
        center.setAlignment(Pos.CENTER_LEFT);
        center.getChildren().addAll(grid, overlayLayer, sideBar);

        return center;
	}

    private Rectangle initOverlayLayer() {
        Rectangle rect = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        rect.setFill(Color.rgb(20, 20, 20));
        rect.setOpacity(0.45);
        rect.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                animateSideBar();
            }
        });
        return rect;
    }

    private GridView<ImageResult> initGridView() {
        final GridView<ImageResult> grid = new GridView<ImageResult>();

        for(int i = 0; i < 25; i++){
            imageList.add(new ImageResult());
        }

        grid.setItems(imageList);
        grid.setId("grid");
        grid.setCellWidth(SCREEN_WIDTH / 4.5);
        grid.setCellHeight(SCREEN_WIDTH / 4.5);
        grid.setCellFactory(new Callback<GridView<ImageResult>, GridCell<ImageResult>>() {
            @Override
            public GridCell<ImageResult> call(GridView<ImageResult> imageResultGridView) {
                return new ImageResultCell(grid.getCellWidth(), grid.getCellHeight());
            }
        });

        return grid;
    }
	
	private Pane initSideBar() {
		Pane left = new AnchorPane();
		left.setId("sideBar");
        left.setMaxWidth(SCREEN_WIDTH / 4.5);
		left.setPrefWidth(SCREEN_WIDTH / 4.5);

		thumbNail = new ImageView();
		thumbNail.setFitHeight(SCREEN_HEIGHT / 3.5);
        Image defaultImage = new Image(getClass().getResourceAsStream("./Images/EmptyImage.png"));
        defaultImage = resizeImageWithSpecifiedHeight(defaultImage, SCREEN_HEIGHT / 3.5);
        thumbNail.setImage(defaultImage);
        thumbNail.setId("thumbNail");
		
		GridPane grid = new GridPane();
		grid.setPrefWidth(left.getPrefWidth());
        grid.setVgap(SCREEN_HEIGHT / 60.0);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(30);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(70);
        grid.getColumnConstraints().addAll(column1, column2);

		Label imageNameTitleLabel = new Label("Name: ".toUpperCase());
		addStyleClass(imageNameTitleLabel, "bold sideBarLabel");
		imageNameLabel = new Label();
        grid.add(imageNameTitleLabel, 0, 0);
        grid.add(imageNameLabel, 1, 0);

		Label imageExtensionTitleLabel = new Label("Ext: ".toUpperCase());
        addStyleClass(imageExtensionTitleLabel, "bold sideBarLabel");
		imageExtensionLabel = new Label();
        grid.add(imageExtensionTitleLabel, 0, 1);
        grid.add(imageExtensionLabel, 1, 1);

		Label imageSizeTitleLabel = new Label("Size: ".toUpperCase());
        addStyleClass(imageSizeTitleLabel, "bold sideBarLabel");
		imageSizeLabel = new Label();
        grid.add(imageSizeTitleLabel, 0, 2);
        grid.add(imageSizeLabel, 1, 2);

        checkBoxForHistogram = new CheckBox("Histogram".toUpperCase());
        checkBoxForSIFT = new CheckBox("Visual Word".toUpperCase());
        checkBoxForConcept = new CheckBox("Visual Concept".toUpperCase());
        VBox subBox = new VBox();
        subBox.setPrefWidth(left.getPrefWidth());
        subBox.setSpacing(SCREEN_HEIGHT / 35.0);
        subBox.setAlignment(Pos.CENTER_LEFT);
        subBox.getChildren().addAll(checkBoxForHistogram, checkBoxForSIFT, checkBoxForConcept);

		VBox box = new VBox();
		box.setPrefWidth(left.getPrefWidth());
		box.setAlignment(Pos.CENTER);
		box.setSpacing(SCREEN_HEIGHT / 50.0);
		box.getChildren().addAll(thumbNail, grid, subBox);

		left.getChildren().add(box);		
		AnchorPane.setTopAnchor(box, SCREEN_HEIGHT / 25.0);
		AnchorPane.setLeftAnchor(box, SCREEN_WIDTH / 200.0);

		return left;
	}

    private void addStyleClass(Node node, String styleClass){
        String[] classes = styleClass.split("\\s+");
        for(String singleClass : classes){
            node.getStyleClass().add(singleClass);
        }
    }
	
	private Image resizeImageWithSpecifiedWidth(Image image, double width) {
		double originalWidth = image.getWidth();
		double originalHeight = image.getHeight();
		
		double height = (originalHeight / originalWidth) * width;
		
		BufferedImage img = new BufferedImage((int) originalWidth, (int) originalHeight, BufferedImage.TYPE_INT_ARGB);
		SwingFXUtils.fromFXImage(image, img);
		BufferedImage rescaled = Scalr.resize(img, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,
	               (int) width, (int) height, Scalr.OP_ANTIALIAS);
		
		WritableImage rescaledFX = new WritableImage((int)width, (int)height);
		SwingFXUtils.toFXImage(rescaled, rescaledFX);
		
		return rescaledFX;
	}
	
	private Image resizeImageWithSpecifiedHeight(Image image, double height) {
		double originalWidth = image.getWidth();
		double originalHeight = image.getHeight();
		
		double width = (originalWidth / originalHeight) * height;
		
		BufferedImage img = new BufferedImage((int) originalWidth, (int) originalHeight, BufferedImage.TYPE_INT_ARGB);
		SwingFXUtils.fromFXImage(image, img);
		BufferedImage rescaled = Scalr.resize(img, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT,
	               (int) width, (int) height, Scalr.OP_ANTIALIAS);
		
		WritableImage rescaledFX = new WritableImage((int)width, (int)height);
		SwingFXUtils.toFXImage(rescaled, rescaledFX);
		
		return rescaledFX;
	}
}
