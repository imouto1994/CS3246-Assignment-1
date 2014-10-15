package FindIO;

import javafx.application.Application;
import java.io.*;
import java.util.*;
import javafx.stage.Stage;

public class FindIOController extends Application implements  FindIOImageChooserInterface, FindIOSearchInterface {
	private FindIOView findIOView;
    private double[] colorHist = null;
    private double[] terms = null;
    private double[] visualWords = null;
    private double[] visualConcepts = null;
    private Map<String, String> imageFilePaths = null;

	@Override
	public void start(Stage primaryStage) {
        initDb();
		findIOView = new FindIOView(primaryStage);
		findIOView.initGUI();
        injectLogicIntoView();
	}

    public void initDb() {
        imageFilePaths = new HashMap<String, String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("src/FindIO/image_groundTruth.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Ground truth file does not exist");
        }
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                String[] img_folders = line.split("\\s+");
                String imageID = img_folders[0];
                String folderName = img_folders[1];
                String filePath = "src/FindIO/Datasets/train/data/"+folderName+"/"+ imageID;
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
        /* Extract Color Hist */
        colorHist = null;
        visualWords = null;
        visualConcepts = null;

        try {
            colorHist = extractHistogram(file);
        } catch (Exception e) {
            System.out.println("There is problem in retrieving histogram for the picture");
        }

        /* Extract Visual Words */
        try {
            visualWords = extractVisualWords(file);
        } catch (Exception e) {
            System.err.println("There is problem in retrieving visual words for the picture");
        }

        /* Extract Visual Concepts */
        try {
            visualConcepts = extractVisualConcepts(file);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("There is problem in retrieving visual concepts for the picture");
        }
    }

    private double[] extractHistogram(File file) throws Exception {
        String imageID = Common.removeExtension(file.getName());
        ColorHistCache colorHistCache = new ColorHistCache();
        double[] result = null;
        Map<String, double[]> cacheResults = colorHistCache.searchImgHist(imageID);
        if(cacheResults.containsKey(imageID)){
            result = cacheResults.get(imageID);
        } else {
            result = ColorHistExtraction.getHist(file);
        }
        return result;
    }

    private double[] extractVisualWords(File file) throws Exception{
        String imageID = Common.removeExtension(file.getName());
        VisualWordCache vwCache = new VisualWordCache();
        double[] result = null;
        Map<String, double[]> cacheResults = vwCache.searchVisualWordsForImage(imageID);
        if(cacheResults.containsKey(imageID)){
            result = cacheResults.get(imageID);
        } else {
            result = VisualWordExtraction.getVisualWords(file);
        }
        return result;
    }

    private double[] extractVisualConcepts(File file) throws Exception {
        String imageID = Common.removeExtension(file.getName());
        VisualConceptCache vcCache = new VisualConceptCache();
        double[] result = null;
        Map<String, double[]> cacheResults = vcCache.searchVisualConceptsForImage(imageID);
        if(cacheResults.containsKey(imageID)) {
            result = cacheResults.get(imageID);
        } else {
            result = VisualConceptExtraction.getVisualConcepts(file);
        }
        return result;
    }

    private String extractTerms() {
        String text = findIOView.getTextField().getText().trim();
        String[] termStrings = new String[10];
        try {
            TextAnalyzer analyzer = new TextAnalyzer();
            String filteredText = analyzer.filterWords(text);
            termStrings = filteredText.split("\\s+");
        } catch (IOException e){
            System.out.println(Common.MESSAGE_TEXT_ANALYZER_ERROR);
            e.printStackTrace();
        }
        Set<String> termsSet = new HashSet<String>(Arrays.asList(termStrings));
        this.terms = new double[termsSet.size()];
        for(int i = 0; i < terms.length; i++){
            this.terms[i] = 1.0;
        }
        StringBuilder strBuilder = new StringBuilder();
        for(String term : termsSet){
            strBuilder.append(term);
            strBuilder.append(" ");
        }

        return strBuilder.toString().trim();
    }

    public List<String> search() {
        boolean hasColorHistogramFeature = findIOView.getCheckBoxForHistogram().isSelected() && colorHist != null;
        boolean hasVisualWordFeature = findIOView.getCheckBoxForSIFT().isSelected() && visualWords != null;
        boolean hasVisualConceptFeature = findIOView.getCheckBoxForConcept().isSelected() && visualConcepts != null;
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
            if(hasVisualWordFeature || hasVisualConceptFeature || hasTextFeature){
                colorHistResults = searchColorHistogram(new ArrayList<String>(imagePool));
            } else {
                colorHistResults = searchAllColorHistograms();
                imagePool.addAll(colorHistResults.keySet());
            }
        }

        List<FindIOPair> rankList = new ArrayList<FindIOPair>();
        for(String image : imagePool){
            double similarity = calculateSimilarity(image, colorHistResults, textResults, vcResults, vwResults);
            rankList.add(new FindIOPair(image, similarity));
        }
        rankList.sort(new Comparator<FindIOPair>() {
            @Override
            public int compare(FindIOPair o1, FindIOPair o2) {
                return -1 * new Double(o1.getValue()).compareTo(new Double(o2.getValue()));
            }
        });

        List<String> results = new ArrayList<String>();
        for(int i = 0; i < rankList.size() && i < Common.MAX_RESULTS; i++){
            results.add(imageFilePaths.get(rankList.get(i).getID()));
        }

        return results;
    }

    private double calculateSimilarity(
            String image,
            Map<String, double[]> colorHistResults,
            Map<String, double[]> textResults,
            Map<String, double[]> vcResults,
            Map<String, double[]> vwResults){
        boolean hasColorHistogramFeature = findIOView.getCheckBoxForHistogram().isSelected() && colorHist != null;
        boolean hasVisualWordFeature = findIOView.getCheckBoxForSIFT().isSelected() && visualWords != null;
        boolean hasVisualConceptFeature = findIOView.getCheckBoxForConcept().isSelected() && visualConcepts != null;
        boolean hasTextFeature = !findIOView.getTextField().getText().trim().isEmpty();

        double[] weights = retrieveWeights(hasColorHistogramFeature, hasTextFeature,hasVisualConceptFeature, hasVisualWordFeature);
        double colorHistSim, textSim, vcSim, vwSim;
        colorHistSim = textSim = vcSim = vwSim = 0.0;

        if(hasColorHistogramFeature && colorHistResults.containsKey(image)){
            colorHistSim = Common.calculateSimilarity(colorHist, colorHistResults.get(image));
        }

        if(hasTextFeature && textResults.containsKey(image)){
            textSim = Common.calculateSimilarity(terms, textResults.get(image));
        }

        if(hasVisualConceptFeature && vcResults.containsKey(image)){
            vcSim = Common.calculateSimilarity(visualConcepts, vcResults.get(image));
        }

        if(hasVisualWordFeature && vwResults.containsKey(image)) {
            vwSim = Common.calculateSimilarity(visualWords, vwResults.get(image));
        }

        return weights[Common.COLOR_HIST_WEIGHT_INDEX] * colorHistSim
                + weights[Common.TEXT_WEIGHT_INDEX] * textSim
                + weights[Common.VISUAL_CONCEPT_WEIGHT_INDEX] * vcSim
                + weights[Common.VISUAL_WORD_WEIGHT_INDEX] * vwSim;
    }

    private double[] retrieveWeights(
            boolean hasColorHistogramFeature,
            boolean hasTextFeature,
            boolean hasVisualConceptFeature,
            boolean hasVisualWordFeature) {

        if(hasColorHistogramFeature && !hasTextFeature && !hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{1.0, 0.0, 0.0, 0.0}; // only histogram
        } else if(!hasColorHistogramFeature && hasTextFeature && !hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.0, 1.0, 0.0, 0.0}; // only text
        } else if(!hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.0, 0.0, 1.0, 0.0}; // only visual concept
        } else if(!hasColorHistogramFeature && !hasTextFeature && !hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.0, 0.0, 0.0, 1.0}; // only visual word
        } else if(hasColorHistogramFeature && hasTextFeature && !hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.25, 0.75, 0.0, 0.0}; // only hist and text
        } else if(hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.25, 0.0, 0.75, 0.0}; // only hist and visual concept
        } else if(hasColorHistogramFeature && !hasTextFeature && !hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.25, 0.0, 0.0, 0.75}; // only hist and visual word
        } else if(!hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.0, 0.6, 0.4, 0.0}; // only text and visual concept
        } else if(!hasColorHistogramFeature && hasTextFeature && !hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.0, 0.4, 0.0, 0.6}; // only text and visual word
        } else if(!hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.0, 0.35, 0.0, 0.65}; // only visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.1, 0.55, 0.35, 0.0}; // only hist, text and visual concept
        } else if(!hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.2, 0.0, 0.2, 0.6}; // only text, visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && !hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.4, 0.0, 0.5}; // only hist, text and visual word
        } else if(hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.3, 0.0, 0.6}; // only hist, visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.2, 0.2, 0.5}; // all features
        }

        System.out.println("Invalid case");
        return null;
    }
    private Map<String, double[]> searchVisualWord(){
        List<FindIOPair> wordsList = new ArrayList<FindIOPair>();
        int index = 0;
        for(int i = 0; i < visualWords.length; i++){
            if(visualWords[i] > 0){
                wordsList.add(new FindIOPair(String.valueOf(i), visualWords[i]));
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
            e.printStackTrace();
            System.out.println("There was error in searching in the index database for color histogram");
        }
        return results;
    }

    private Map<String, double[]> searchAllColorHistograms() {
        ColorHistIndex colorHistIndex = new ColorHistIndex();
        Map<String, double[]> results = null;
        try {
            results = colorHistIndex.scanImgHist();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("There was error in searching in the index database for color histogram");
        }
        return results;
    }
}
