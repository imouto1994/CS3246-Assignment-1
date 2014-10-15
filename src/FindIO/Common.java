package FindIO;

/**
 * Created by Beyond on 10/12/2014 0012.
 */
public class Common {
    static final String MESSAGE_TEXT_INDEX_ERROR = "Encounter some errors when indexing text annotations";
    static final String MESSAGE_TEXT_SEARCH_ERROR = "Some errors when searching text annotations";
    static final String MESSAGE_HIST_INDEX_ERROR = "Encounter some errors when indexing image color histogram";
    static final String MESSAGE_HIST_SEARCH_ERROR = "Some errors when searching image color histogram";
    static final String MESSAGE_VC_INDEX_ERROR = "Encounter some errors when indexing image visual concepts";
    static final String MESSAGE_VC_SEARCH_ERROR = "Some errors when searching image visual concepts";
    static final String MESSAGE_FILE_NOTEXIST = "Woops! File not existing";
    static final String MESSAGE_TEXT_ANALYZER_ERROR = "Loading text analyzer failed";

    public static final int topK = 1024;
    public static final int NUM_VISUAL_WORDS = 21504;
    public static final int NUM_VISUAL_CONCEPTS = 1000;
    public static final int MAXIMUM_NUMBER_OF_TERMS = 1000;

    public static String removeExtension(String fileName){
        if(!fileName.contains(".")){
            return fileName;
        } else {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
    }
}
