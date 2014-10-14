package FindIO;

import java.io.*;
import java.util.Arrays;

public class VisualConceptExtraction {

    public static void main(String[] args){
        processFileList("demolist.txt");
    }

    public static void processFileList(String fileListName) {
        File file = new File("src/FindIO/Features/Visual Concept");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "/wait", "image_classification.exe", fileListName);
        builder.directory(file);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            process.waitFor();
        } catch (Exception e) {
            System.out.println("There is problem in running the process");
        }
    }

    public static double[] getVisualConcepts(File file){

        String filePath = file.getAbsolutePath();

        File textFile = new File("src/FindIO/Features/Visual Concept/queryList.txt");
        if(!textFile.exists()){
            try {
                textFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Cannot create a new file");
            }
        }

        try {
            FileWriter fw = new FileWriter(textFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(filePath);
            bw.close();
        } catch (IOException e) {
            System.out.println("Failed to write file");
        }

        File featureDir = new File("src/FindIO/Features/Visual Concept");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "/wait", "image_classification.exe", "queryList.txt");
        builder.directory(featureDir);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            process.waitFor();
            process.destroy();
        } catch (Exception e) {
            System.out.println("There is problem in running the process");
        }

        File resultFile = new File(filePath.substring(0, filePath.lastIndexOf('.') + 1) + "txt");

        double[] concepts = new double[Common.NUM_VISUAL_CONCEPTS];
        try {
            BufferedReader br = new BufferedReader(new FileReader(resultFile.getAbsolutePath()));
            String line;
            while((line = br.readLine()) != null){
                String[] freqs = line.trim().split("\\s+");
                for(int i = 0; i < freqs.length; i++){
                    concepts[i] = Double.parseDouble(freqs[i]);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Result file is not created");
        } catch (IOException e) {
            System.out.println("Cannot read line from results");
        }

        resultFile.delete();

        System.out.println(Arrays.toString(concepts));

        return concepts;
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
