package FindIO;

import java.io.File;

public class VisualWordExtraction {

    public static void main(String[] args){
        initializeProcess();
    }

    public static void initializeProcess() {
        File file = new File("src/FindIO/Features/Visual Concept");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "image_classification.exe", "demolist.txt");
        builder.directory(file);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: implement this method
    public static double[] getVisualWords(File file){
        return null;
    }

    // TODO: implement this method
    public static double calculateSimilarity(double[] arr1, double[] arr2){
        return 1;
    }

    // TODO: implement this method
    public static double calculateDistance(double[] arr1, double[] arr2){
        return 0;
    }
}
