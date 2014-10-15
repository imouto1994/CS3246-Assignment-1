package FindIO;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;

public class VisualConceptExtraction {

    // TODO: implement this method
    public static void main(String[] args){
        String imageListFileName = "trainImageList.txt";
        String dataFolder = "./src/FindIO/Datasets/train/data";
        String demoFile = "demolist.txt";
        getVisualConceptsForImages(imageListFileName);
//        try {
//            generateImageList(imageListFileName, dataFolder);
//        } catch (Throwable e){
//            System.out.println("error occurs when writing to image list file");
//            e.printStackTrace();
//        }
    }

    public static void getVisualConceptsForImages(String fileListName) {
        File file = new File("src/FindIO/Features/Visual Concept");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "/wait", "image_classification.exe", fileListName);
        builder.directory(file);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            process.waitFor();
            System.out.println("Finish retreiving visual concepts in image list");
        } catch (Exception e) {
            System.out.println("There is problem in running the process");
            e.printStackTrace();
        }
    }

    public static void generateImageList(String imageListFileName, String dataFolder) {
        FileWriter fileWriter = null;
        File imageListFile = new File(imageListFileName);
        if(!imageListFile.exists()){
            try {
                imageListFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Cannot create image list file");
            }
        }
        try {
            fileWriter = new FileWriter(new File(imageListFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuffer strbuf = new StringBuffer();

        File file = new File(dataFolder);
        File[] imageFolders = file.listFiles();

        for(int i = 0; i < imageFolders.length; i++) {
            File folder = imageFolders[i];
            if(folder.exists() && folder.isDirectory()){
                File[] images = folder.listFiles();
                for(File image : images){
                    if(image.exists() && !image.isDirectory() && !image.getName().endsWith("txt")){
                        strbuf.append(image.getAbsolutePath());
                        strbuf.append(String.format("%n"));
                    }
                }
            }
        }
        try {
            fileWriter.write(strbuf.toString());
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("There was error in writing to image list");
        }

        System.out.println("Finish generating for " + imageListFileName);
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

        return concepts;
    }
}
