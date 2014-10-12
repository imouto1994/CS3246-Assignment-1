package FindIO;

import java.io.File;

public class VisualConceptExtraction {

    // TODO: implement this method
    public static void main(String[] args){
        initializeProcess("demolist.txt");
    }

    public static void initializeProcess(String fileListName) {
        File file = new File("src/FindIO/Features/Visual Concept");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "image_classification.exe", fileListName);
        builder.directory(file);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: implement this method
    public static double calculateSimilarity(double[] arr1, double[] arr2){
        return 1;
    }

    // TODO: implement this method
    public static double calculateDistance(double[] arr1, double[] arr2) {
        return 0;
    }
}
