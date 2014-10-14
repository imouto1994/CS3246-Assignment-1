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
    private Map<String, String> imageFilePaths = null;

	@Override
	public void start(Stage primaryStage) {
        //initDb();
		findIOView = new FindIOView(primaryStage);
		findIOView.initGUI();
        injectLogicIntoView();
	}

    public void initDb() {
        imageFilePaths = new HashMap<String, String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("src/FindIO/image_mainGroundTruth.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Ground truth file does not exist");
        }
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                String[] img_folders = line.split("\\s+");
                String imageID = img_folders[0];
                String folderName = img_folders[1];
                String filePath = "./src/FindIO/Datasets/train/data/"+folderName+"/"+ imageID;
                imageFilePaths.put(Common.removeExtension(imageID), filePath);
            }
        } catch (IOException e) {
            System.out.println("There was error during the process of reading the ground truth file");
        }
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
            e.printStackTrace();
            System.out.println("There is problem in retrieving histogram for the picture");
        }

        /* Extract Visual Words */
        visualWords = extractVisualWords(file);

        /* Extract Visual Concepts */
        visualConcepts = extractVisualConcepts(file);
    }

    private double[] extractHistogram(File file) throws Exception {
        return ColorHistExtraction.getHist(file);
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

    public List<String> search() {
        boolean hasColorHistogramFeature = findIOView.getCheckBoxForHistogram().isSelected();
        boolean hasVisualWordFeature = findIOView.getCheckBoxForSIFT().isSelected();
        boolean hasVisualConceptFeature = findIOView.getCheckBoxForConcept().isSelected();
        boolean hasTextFeature = !findIOView.getTextField().getText().trim().isEmpty();

        boolean isSearchValid = hasColorHistogramFeature || hasVisualWordFeature || hasVisualConceptFeature || hasTextFeature;
        if(!isSearchValid){
            return null;
        }

        Set<String> imagePool = new HashSet<String>();

        Map<String, double[]> vwResults = null;
        if(hasVisualWordFeature ){
            vwResults  = searchVisualWord();
            imagePool.addAll(vwResults.keySet());
        }
        Map<String, double[]> vcResults = null;
        if(hasVisualConceptFeature){
            vcResults = searchVisualConcept();
            imagePool.addAll(vcResults.keySet());
        }
        Map<String, double[]> textResults = null;
        if(hasTextFeature) {
            textResults = searchText();
            imagePool.addAll(textResults.keySet());
        }

        Map<String, double[]> colorHistResults = null;
        if(hasColorHistogramFeature){
            if(hasColorHistogramFeature || hasVisualConceptFeature || hasTextFeature){
                colorHistResults = searchColorHistogram(new ArrayList<String>(imagePool));
            } else {
                colorHistResults = searchAllColorHistograms();
                imagePool.addAll(colorHistResults.keySet());
            }
        }

        return null;
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
        StringBuilder strBuilder = new StringBuilder();
        for(int i = 0; i < visualConcepts.length; i++){
            if(visualConcepts[i] > 0){
                strBuilder.append(String.valueOf(i));
                strBuilder.append(" ");
            }
        }
        VisualConceptIndex vcIndex = new VisualConceptIndex();
        Map<String, double[]> results = null;
        try {
            results = vcIndex.searchVisualConcept(strBuilder.toString().trim());
        } catch (Exception e) {
            System.out.println("There was error in searching in the index database for visual concepts");
        }

        return results;
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

    private Map<String, double[]> searchColorHistogram(List<String> images) {
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
