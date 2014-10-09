package FindIO;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
	View view;

	@Override
	public void start(Stage primaryStage) {
		view = new View(primaryStage);
		view.initGUI();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
