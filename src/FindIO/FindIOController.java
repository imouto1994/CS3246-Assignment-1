package FindIO;

import javafx.animation.ParallelTransition;
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
        /* Extract Color Histogram
        try {
            double[] colorHist = extractHistogram(file);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        */

        /* Extract Terms
        String[] extractedTerms = extractTerms();
        */

        /* Extract Visual Words */
        double[] visualWords = extractVisualWords(file);


        /* Extract Visual Concepts
        double[] visualConcepts = extractVisualConcepts(file);
        */
    }

    private double[] extractHistogram(File file) throws Throwable {
        return ColorHistogramExtraction.getHist(file);
    }

    private double[] extractVisualWords(File file) {
        return VisualWordExtraction.getVisualWords(file);
    }

    private double[] extractVisualConcepts(File file) {
        return VisualConceptExtraction.getVisualConcepts(file);
    }

    private String[] extractTerms() {
        String[] terms = findIOView.getTextField().getText().trim().split("\\s+");

        //TODO: Add removing stop words

        return terms;
    }
}
