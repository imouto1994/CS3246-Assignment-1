package FindIO;

import javafx.application.Application;
import java.io.*;
import java.util.*;

import javafx.stage.Stage;

public class FindIOController extends Application implements  FindIOImageChooserInterface, FindIOSearchInterface {
	private FindIOView findIOView;
    private double[] colorHist = null;
    private double[] visualWords = null;
    private double[] visualConcepts = null;

	@Override
	public void start(Stage primaryStage) {
		findIOView = new FindIOView(primaryStage);
		findIOView.initGUI();
        injectLogicIntoView();
	}

    public void injectLogicIntoView() {
        findIOView.linkImageChooser(this);
        findIOView.linkSearch(this);
    }

	public static void main(String[] args) {
		launch(args);
	}

    public void imageSelectHandle(File file) {
        /* Extract Color Histogram */
        colorHist = null;
        visualWords = null;
        visualConcepts = null;

        try {
            colorHist = extractHistogram(file);
        } catch (Exception e) {
            System.out.println("There is problem in retrieving histogram for the picture");
        }

        /* Extract Visual Words */
        visualWords = extractVisualWords(file);

        /* Extract Visual Concepts */
        visualConcepts = extractVisualConcepts(file);
    }

    private double[] extractHistogram(File file) throws Exception {
        return ColorHistogramExtraction.getHist(file);
    }

    private double[] extractVisualWords(File file) {
        return VisualWordExtraction.getVisualWords(file);
    }

    private double[] extractVisualConcepts(File file) {
        return VisualConceptExtraction.getVisualConcepts(file);
    }

    private String extractTerms() {
        String[] terms = findIOView.getTextField().getText().trim().split("\\s+");

        //TODO: Add removing stop words
        Set<String> termsSet = new HashSet<String>(Arrays.asList(terms));
        StringBuilder strBuilder = new StringBuilder();
        String queryTerm = "";
        for(String term : termsSet){
            strBuilder.append(term);
            strBuilder.append(" ");
        }

        return strBuilder.toString().trim();
    }

    public void search() {
        boolean hasColorHistogramFeature = findIOView.getCheckBoxForHistogram().isSelected();
        boolean hasVisualWordFeature = findIOView.getCheckBoxForSIFT().isSelected();
        boolean hasVisualConceptFeature = findIOView.getCheckBoxForConcept().isSelected();
        boolean hasTextFeature = !findIOView.getTextField().getText().trim().isEmpty();

        if(hasVisualWordFeature){
            Map<String, double[]> vwResults = searchVisualWord();
        }

        if(hasTextFeature) {
            Map<String, double[]> textResults = searchText();
        }
    }

    private Map<String, double[]> searchVisualWord(){
        List<FindIOPair> wordsList = new ArrayList<FindIOPair>();
        int index = 0;
        for(double freq : visualWords){
            if(freq > 0){
                wordsList.add(new FindIOPair(String.valueOf(index++), freq));
            }
        }
        Collections.sort(wordsList);
        StringBuilder strBuilder = new StringBuilder();
        for(int i = wordsList.size() - 1; i >= 0 && (wordsList.size() - i) <= Common.MAXIMUM_NUMBER_OF_TERMS; i--){
            FindIOPair word = wordsList.get(i);
            strBuilder.append(word.getID());
            strBuilder.append(" ");
        }
        VisualWordIndex vwIndex = new VisualWordIndex();
        Map<String, double[]> results = null;
        try {
            results = vwIndex.searchVisualWord(strBuilder.toString().trim());
        } catch (Exception e) {
            System.out.println("There was error in searching in the index database for visual words");
        }

        return results;
    }

    private Map<String, double[]> searchVisualConcept(){
        return null;
    }

    private Map<String, double[]> searchText() {
        String queryString = extractTerms();
        TextIndex textIndex = new TextIndex();
        Map<String, double[]> results = null;
        try {
            results = textIndex.searchText(queryString);
        } catch (Exception e) {
            System.out.println("There was error in searching in the index database for text annotation");
        }
        return results;
    }

    private Map<String, double[]> searchColorHistogram(String[] images) {
        StringBuilder strBuilder = new StringBuilder();
        for(String image : images) {
            strBuilder.append(image);
            strBuilder.append(" ");
        }
        ColorHistIndex colorHistIndex = new ColorHistIndex();
        Map<String, double[]> results = null;
        try {
            results = colorHistIndex.searchImgHist(strBuilder.toString().trim());
        } catch (Exception e){
            System.out.println("There was error in searching in the index database for color histogram");
        }
        return results;
    }

    private Map<String, double[]> searchAllColorHistograms() {
        return null;
    }
}
