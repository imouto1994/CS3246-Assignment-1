package FindIO;

import javafx.application.Application;
import java.io.*;
import java.util.*;
import javafx.stage.Stage;

public class FindIOController extends Application implements  FindIOImageChooserInterface, FindIOSearchInterface, RelevanceFeedbackInterface {
	private FindIOView findIOView;
    private double[] colorHist = null;
    private double[] terms = null;
    private double[] visualWords = null;
    private double[] visualConcepts = null;
    private String queryString;
    private Map<String, String> imageFilePaths = null;
    private ColorHistCache colorHistCache;
    private VisualConceptCache visualConceptCache;
    private VisualWordCache visualWordCache;

    private double maxTextSim = 0.0;

    private boolean isCHSelected = false;
    private boolean isVWSelected = false;
    private boolean isVCSelected = false;
    private boolean isTextSelected = false;

	@Override
	public void start(Stage primaryStage) {
        initDb();
		findIOView = new FindIOView(primaryStage, this);
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
        try {
            String line;
            assert reader != null;
            while ((line = reader.readLine()) != null) {
                String[] img_folders = line.split("\\s+");
                String imageID = img_folders[0];
                String folderName = img_folders[1];
                String filePath = "src/FindIO/Datasets/train/data/"+folderName+"/"+ imageID;
                imageFilePaths.put(Common.removeExtension(imageID), filePath);
            }
        } catch (Exception e) {
            System.out.println("There was error during the process of reading the ground truth file");
        }

        /* Initialize Caches */
        colorHistCache = new ColorHistCache();
        visualConceptCache = new VisualConceptCache();
        visualWordCache = new VisualWordCache();
        try {
            colorHistCache.initBuilding();
            visualConceptCache.initBuilding();
            visualWordCache.initBuilding();
        } catch (Throwable throwable) {
            System.err.println("There is error in the process of initializing the caches");
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
        double[] result = null;
        Map<String, double[]> cacheResults = colorHistCache.searchImgHist(imageID);
        if(cacheResults.containsKey(imageID)){
            result = cacheResults.get(imageID);
        } else {
            result = ColorHistExtraction.getHist(file);
            colorHistCache.addDoc(imageID, result);
        }
        return result;
    }

    private double[] extractVisualWords(File file) throws Exception{
        String imageID = Common.removeExtension(file.getName());
        double[] result = null;
        Map<String, double[]> cacheResults = visualWordCache.searchVisualWordsForImage(imageID);
        if(cacheResults.containsKey(imageID)){
            result = cacheResults.get(imageID);
        } else {
            result = VisualWordExtraction.getVisualWords(file);
            visualWordCache.addDoc(imageID, result);
        }
        return result;
    }

    private double[] extractVisualConcepts(File file) throws Exception {
        String imageID = Common.removeExtension(file.getName());
        double[] result = null;
        Map<String, double[]> cacheResults = visualConceptCache.searchVisualConceptsForImage(imageID);
        if(cacheResults.containsKey(imageID)) {
            result = cacheResults.get(imageID);
        } else {
            result = VisualConceptExtraction.getVisualConcepts(file);
            visualConceptCache.addDoc(imageID, result);
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
        boolean isCHSelected = findIOView.getCheckBoxForHistogram().isSelected();
        boolean isVWSelected = findIOView.getCheckBoxForSIFT().isSelected();
        boolean isVCSelected = findIOView.getCheckBoxForConcept().isSelected();
        boolean isTextSelected = !findIOView.getTextField().getText().trim().isEmpty();

        return search(isCHSelected,  isVWSelected, isVCSelected,  isTextSelected);
    }

    public List<String> search(boolean isCHSelected, boolean isVWSelected, boolean isVCSelected, boolean isTextSelected) {

        this.isCHSelected = isCHSelected;
        this.isVWSelected = isVWSelected;
        this.isVCSelected = isVCSelected;
        this.isTextSelected = isTextSelected;

        boolean hasColorHistogramFeature = isCHSelected && colorHist != null;
        boolean hasVisualWordFeature = isVWSelected && visualWords != null;
        boolean hasVisualConceptFeature = isVCSelected && visualConcepts != null;
        boolean isSearchValid = hasColorHistogramFeature || hasVisualWordFeature || hasVisualConceptFeature || isTextSelected;
        if(!isSearchValid){
            return new ArrayList<String>();
        }

        Set<String> imagePool = new HashSet<String>();

        Map<String, double[]> vwResults = null;
        if(hasVisualWordFeature ){
            vwResults  = searchVisualWordsForAllImages();
            List<String> vwPool = filterVWResults(visualWords, vwResults);
            imagePool.addAll(vwPool);
        }
        Map<String, double[]> vcResults = null;
        if(hasVisualConceptFeature){
            vcResults = searchVisualConcept();
            imagePool.addAll(vcResults.keySet());
        }
        Map<String, double[]> textResults = null;
        if(isTextSelected) {
            textResults = searchText();
            imagePool.addAll(textResults.keySet());
        }

        Map<String, double[]> colorHistResults = null;
        if(hasColorHistogramFeature){
            if(hasVisualWordFeature || hasVisualConceptFeature || isTextSelected){
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
        maxTextSim = 0.0;
        rankList.sort(new Comparator<FindIOPair>() {
            @Override
            public int compare(FindIOPair o1, FindIOPair o2) {
                return -1 * new Double(o1.getValue()).compareTo(o2.getValue());
            }
        });

        List<String> results = new ArrayList<String>();
        for(int i = 0; i < rankList.size() && i < Common.MAX_RESULTS; i++){
            results.add(imageFilePaths.get(rankList.get(i).getID()));
        }

        return results;
    }

    private List<String> filterVWResults(double[] visualWords, Map<String, double[]> vwResults) {
        List<FindIOPair> top40 = new ArrayList<FindIOPair>();
        for(String imageID: vwResults.keySet()){
            double[] imageWords = vwResults.get(imageID);
            top40.add(new FindIOPair(imageID, Common.calculateSimilarity(visualWords, imageWords, Common.CORRELATION_DISTANCE)));
        }
        Collections.sort(top40);
        List<String> pool = new ArrayList<String>();
        for(int i = top40.size() - 1; i >= (top40.size() - 40); i--){
            pool.add(top40.get(i).getID());
        }

        return pool;
    }

    private double calculateSimilarity(
            String image,
            Map<String, double[]> colorHistResults,
            Map<String, double[]> textResults,
            Map<String, double[]> vcResults,
            Map<String, double[]> vwResults){

        boolean hasColorHistogramFeature = this.isCHSelected && colorHist != null;
        boolean hasVisualWordFeature = this.isVWSelected && visualWords != null;
        boolean hasVisualConceptFeature = this.isVCSelected && visualConcepts != null;
        boolean hasTextFeature = this.isTextSelected;

        double[] weights = retrieveWeights(hasColorHistogramFeature, hasTextFeature,hasVisualConceptFeature, hasVisualWordFeature);
        double colorHistSim, textSim, vcSim, vwSim;
        colorHistSim = textSim = vcSim = vwSim = 0.0;

        if(hasColorHistogramFeature && colorHistResults.containsKey(image)){
            colorHistSim = Common.calculateSimilarity(colorHist, colorHistResults.get(image), Common.CORRELATION_DISTANCE);
        }

        if(hasTextFeature && textResults.containsKey(image)){
            textSim = Common.calculateSimilarity(terms, textResults.get(image), Common.CORRELATION_DISTANCE_WITHOUT_NORMALIZATION);
            if(maxTextSim > 0.0){
                textSim = textSim / maxTextSim;
            }
        }

        if(hasVisualConceptFeature && vcResults.containsKey(image)){
            vcSim = Common.calculateSimilarity(visualConcepts, vcResults.get(image), Common.BHATTACHARYYA_DISTANCE);
        }

        if(hasVisualWordFeature && vwResults.containsKey(image)) {
            vwSim = Common.calculateSimilarity(visualWords, vwResults.get(image), Common.CORRELATION_DISTANCE);
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
            return new double[]{0.0, 0.6, 0.0, 0.4}; // only text and visual word
        } else if(!hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.0, 0.0, 0.55, 0.45}; // only visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && !hasVisualWordFeature){
            return new double[]{0.1, 0.55, 0.35, 0.0}; // only hist, text and visual concept
        } else if(!hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.0, 0.4, 0.4, 0.3}; // only text, visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && !hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.5, 0.0, 0.4}; // only hist, text and visual word
        } else if(hasColorHistogramFeature && !hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.0, 0.5, 0.4}; // only hist, visual concept and visual word
        } else if(hasColorHistogramFeature && hasTextFeature && hasVisualConceptFeature && hasVisualWordFeature){
            return new double[]{0.1, 0.4, 0.3, 0.2}; // all features
        }

        System.out.println("Invalid case");
        return null;
    }

    private Map<String, double[]> searchVisualWordsForAllImages(){
        VisualWordIndex visualWordIndex = new VisualWordIndex();
        Map<String, double[]> results = null;
        try {
            results = visualWordIndex.scanVisualWords();
        } catch (Exception e) {
            results = new HashMap<String, double[]>();
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
            results = new HashMap<String, double[]>();
            System.out.println("There was error in searching in the index database for visual concepts");
        }

        return results;
    }

    private Map<String, double[]> searchText() {
        queryString = extractTerms();
        TextIndex textIndex = new TextIndex();
        Map<String, double[]> results = null;
        try {
            results = textIndex.searchText(queryString);
        } catch (Exception e) {
            results = new HashMap<String, double[]>();
            e.printStackTrace();
            System.out.println("There was error in searching in the index database for text annotation");
        }
        for(double[] termsFreq: results.values()){
            double textSim = Common.calculateSimilarity(terms, termsFreq, Common.CORRELATION_DISTANCE_WITHOUT_NORMALIZATION);
            if(textSim > maxTextSim){
                maxTextSim = textSim;
            }
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
            results = new HashMap<String, double[]>();
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
            results = new HashMap<String, double[]>();
            System.out.println("There was error in searching in the index database for color histogram");
        }
        return results;
    }

    public void upvote(String imageID) {
        List<FindIOPair> updateTermsList = new ArrayList<FindIOPair>();
        for(String term : queryString.trim().split("\\s+")){
            updateTermsList.add(new FindIOPair(term, 0.5));
        }
        TextIndex textIndex = new TextIndex();
        try {
            textIndex.initBuilding();
            textIndex.updateScore(imageID, updateTermsList);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.println("There is problem in updating text index");
        }
    }

    public void downvote(String imageID) {
        List<FindIOPair> updateTermsList = new ArrayList<FindIOPair>();
        for(String term : queryString.trim().split("\\s+")){
            updateTermsList.add(new FindIOPair(term, -0.3));
        }
        TextIndex textIndex = new TextIndex();
        try {
            textIndex.initBuilding();
            textIndex.updateScore(imageID, updateTermsList);
        } catch (Throwable throwable) {
            System.err.println("There is problem in updating text index");
        }
    }
}
