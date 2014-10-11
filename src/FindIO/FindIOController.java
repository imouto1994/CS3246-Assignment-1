package FindIO;

import javafx.application.Application;
import java.io.*;

import javafx.stage.Stage;

public class FindIOController extends Application implements  FindIOImageChooserInterface {
	FindIOView findIOView;

	@Override
	public void start(Stage primaryStage) {
		findIOView = new FindIOView(primaryStage);
		findIOView.initGUI();
        injectLogicIntoView();
	}

    public void injectLogicIntoView() {
        findIOView.linkImageChooser(this);
    }

	public static void main(String[] args) {
		launch(args);
	}

    public void imageSelectHandle(File file) {
        double[] colorHist = extractHistogram(file);

    }

    private double[] extractHistogram(File file) {
        return ColorHistogramExtraction.getHist(file);
    }

    private double[] extractVisualWords(File file) {
        return null;
    }

    private double[] extractVisualConcepts(File file) {
        return null;
    }

    private String[] extractTerms() {
        String[] terms = findIOView.getTextField().getText().split("\\s+");

        //TODO: Add removing stop words

        return terms;
    }
}
