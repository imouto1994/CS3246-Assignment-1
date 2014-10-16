package FindIO;

public class Common {

    public static final String MESSAGE_TEXT_INDEX_ERROR = "Encounter some errors when indexing text annotations";
    public static final String MESSAGE_TEXT_UPDATE_ERROR = "Some error occurs when updating text annotation scores";
    public static final String MESSAGE_HIST_SCAN_ERROR = "Some errors occur when scanning all images and retrieve the color histogram";
    public static final String MESSAGE_TEXT_SEARCH_ERROR = "Some errors when searching text annotations";
    public static final String MESSAGE_HIST_INDEX_ERROR = "Encounter some errors when indexing image color histogram";
    public static final String MESSAGE_HIST_SEARCH_ERROR = "Some errors when searching image color histogram";
    public static final String MESSAGE_VC_INDEX_ERROR = "Encounter some errors when indexing image visual concepts";
    public static final String MESSAGE_VC_SEARCH_ERROR = "Some errors when searching image visual concepts";
    public static final String MESSAGE_VW_INDEX_ERROR = "Encounter some errors when indexing image visual words";
    public static final String MESSAGE_VW_SEARCH_ERROR = "Some errors when searching image visual words";
    public static final String MESSAGE_FILE_NOTEXIST = "Woops! File not existing";
    public static final String MESSAGE_FILE_INDEX_SUCCESS = "Index successfully image ";
    public static final String MESSAGE_TEXT_ANALYZER_ERROR = "Some errors when analyzing text";
    public static final String MESSAGE_LOAD_TEXT_ERROR = "Some errors when loading text index";
    public static final String MESSAGE_BENCHMARK_ERROR = "Some errors when benchmarking";


    public static final int topK = 1024;
    public static final int NUM_VISUAL_WORDS = 21504;
    public static final int NUM_VISUAL_CONCEPTS = 1000;
    public static final int MAXIMUM_NUMBER_OF_TERMS = 50;
    public static final int MAX_RESULTS = 20;

    public static final int COLOR_HIST_WEIGHT_INDEX = 0;
    public static final int TEXT_WEIGHT_INDEX = 1;
    public static final int VISUAL_CONCEPT_WEIGHT_INDEX = 2;
    public static final int VISUAL_WORD_WEIGHT_INDEX = 3;

    public static String removeExtension(String fileName){
        if(!fileName.contains(".")){
            return fileName;
        } else {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
    }

    public static String removeLast(String originalString, String removal){
        int index = originalString.lastIndexOf(removal);
        if(index != -1){
            originalString = originalString.substring(0, index);
        }
        return originalString;
    }

    public static double calculateSimilarity(double[] arr1, double[] arr2){
        return 1 - calculateDistance(arr1, arr2);
    }

    public static double calculateDistance(double[] arr1, double[] arr2) {
        // Bhattacharyya distance
        double h1 = 0.0;
        double h2 = 0.0;
        int N = arr1.length;
        for(int i = 0; i < N; i++) {
            h1 = h1 + arr1[i];
            h2 = h2 + arr2[i];
        }

        double Sum = 0.0;
        for(int i = 0; i < N; i++) {
            Sum = Sum + Math.sqrt(arr1[i]*arr2[i]);
        }
        double dist = Math.sqrt(1 - Sum / Math.sqrt(h1*h2));
        return dist;
    }
}
