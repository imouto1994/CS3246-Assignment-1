package FindIO;

/**
 * Created by Beyond on 10/12/2014 0012.
 */
public class Common {
    public static final String MESSAGE_TEXT_INDEX_ERROR = "Encounter some errors when indexing text annotations";
    public static final String MESSAGE_TEXT_SEARCH_ERROR = "Some errors when searching text annotations";
    public static final String MESSAGE_HIST_INDEX_ERROR = "Encounter some errors when indexing image color histogram";
    public static final String MESSAGE_HIST_SEARCH_ERROR = "Some errors when searching image color histogram";
    public static final String MESSAGE_FILE_NOTEXIST = "Woops! File not existing";

    public static final int topK = 1024;

    public static String removeExtension(String fileName){
        if(!fileName.contains(".")){
            return fileName;
        } else {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
    }
}
