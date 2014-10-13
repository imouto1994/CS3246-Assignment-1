package FindIO;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class VisualConceptExtraction {

    // TODO: implement this method
    public static void main(String[] args){
        String imageListFileName = "trainImageList.txt";
        String dataFolder = "./src/FindIO/Datasets/train/data";
        String demoFile = "demolist.txt";
        initializeProcess(imageListFileName);
//        try {
//            generateImageList(imageListFileName, dataFolder);
//        } catch (Throwable e){
//            System.out.println("error occurs when writing to image list file");
//            e.printStackTrace();
//        }
    }

    public static void initializeProcess(String fileListName) {
        File file = new File("./src/FindIO/Features/Visual Concept");
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

    public static void generateImageList(String imageListFileName, String dataFolder) throws Throwable{
        FileWriter fileWriter = new FileWriter(new File(imageListFileName));
        StringBuffer strbuf = new StringBuffer();

        File file = new File(dataFolder);
        File[] imageFolders = file.listFiles();

        for(int i = 0; i < imageFolders.length; i++) {
            File folder = imageFolders[i];
            if(folder.exists() && folder.isDirectory()){
                File[] images = folder.listFiles();
                for(File image : images){
                    if(image.exists() && !image.isDirectory()){

                        //write the image path to the list file
                        String path="../../Datasets/train/data/"+folder.getName()+"/"+image.getName();
                        strbuf.append(path + String.format("%n"));
                    }
                }
            }
        }
        fileWriter.write(strbuf.toString());
        fileWriter.close();
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
