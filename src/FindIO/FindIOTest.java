package FindIO;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FindIOTest {

    FindIOController controller;
    HashMap<String, ArrayList<String>> testGtmap;  //A directive map from image to its ground truth
    HashMap<String, ArrayList<String>> trainGtmap; //A directive map from image to its ground truth

    public FindIOTest(){
        controller = new FindIOController();
        controller.initDb();

        testGtmap = new HashMap<String, ArrayList<String>>();
        trainGtmap = new HashMap<String, ArrayList<String>>();

        try {
            loadImagesGt("./src/FindIO/query_groundTruth.txt", testGtmap);
            loadImagesGt("./src/FindIO/image_groundTruth.txt", trainGtmap);
        } catch(Exception e){
            System.out.println(Common.MESSAGE_LOAD_TEXT_ERROR);
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        FindIOTest tester = new FindIOTest();
        try {
            long start = System.currentTimeMillis();
            tester.evaluateSearch("./src/FindIO/resultLog.txt");
            long end = System.currentTimeMillis();
            System.out.println("Total time: " + (end-start) +"ms");
        } catch(IOException e){
            System.out.println(Common.MESSAGE_BENCHMARK_ERROR);
            e.printStackTrace();
        }
    }

    public void loadImagesGt(String gtFile, HashMap<String,ArrayList<String>> gtMap) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(gtFile));
        String line = null;

        //add the image frequency pair to the tag posting list
        int count = 0;
        while ((line = reader.readLine()) != null) {
            String[] img_tags = line.split(" ");
            String imgID = Common.removeExtension(img_tags[0]);
            ArrayList<String> tagList = new ArrayList<String>();
            for(int i = 1; i < img_tags.length; i ++) {
                tagList.add(img_tags[i]);
            }
            gtMap.put(imgID, tagList);
            count++;
        }

        System.out.println("Number of query images: " + count);
        reader.close();
    }

    public void evaluateSearch(String benchmarkFile) throws IOException{
        FileWriter fileWriter = new FileWriter(new File(benchmarkFile));
        System.out.println("imageName\tch\tvw\tchvw\tvc\tchvc\tvwvc\tchvwvc");
        for(int i = 1; i <= 7; i++){
            boolean isCHSelected, isVWSelected, isVCSelected, isTextSelected;
            if(i % 2 == 1) {
                isCHSelected = true;
            } else {
                isCHSelected = false;
            }
            if(i % 4 == 2 || i % 4 == 3){
                isVWSelected = true;
            } else {
                isVWSelected = false;
            }
            if(i >= 4){
                isVCSelected = true;
            } else {
                isVCSelected = false;
            }
            isTextSelected = false;
            fileWriter.write("CH " + isCHSelected + "  VW " + isVWSelected + "  VC " + isVCSelected + "  TEXT " + isTextSelected + String.format("%n"));
            for(String imageName : testGtmap.keySet()){
                fileWriter.write("\tImage: " + imageName + "\tTag: " + testGtmap.get(imageName).get(1) + String.format("%n"));
                isTextSelected = false;
                List<String> rankedList = searchFile(imageName, isCHSelected, isVWSelected, isVCSelected, isTextSelected);
                double[] scores = getRelevanceScore(imageName, rankedList);
                double precision = (scores[0] / scores[1]);
                double recall = (scores[0] / scores[2]);
                double f1 = (2 * precision * recall) / (precision + recall);
                fileWriter.write("\tPrecision: " + precision + "\tRecall: " + recall + "\tF1: " + f1 + String.format("%n"));
            }
            System.out.println("Done " + i + "!");
            fileWriter.write(String.format("%n") + String.format("%n") + String.format("%n"));
        }
        fileWriter.close();
    }

    public List<String> searchFile(String imageName, boolean isCHSelected, boolean isVWSelected, boolean isVCSelected, boolean isTextSelected){


        ArrayList<String> tagList = testGtmap.get(imageName);
        String tag = tagList.get(0);

        controller.imageSelectHandle(new File("./src/FindIO/Datasets/test/query/"+tag+"/"+imageName+".jpg"));
        List<String> rankedList = controller.search(isCHSelected, isVWSelected, isVCSelected, isTextSelected);
        List<String> rankedFilteredList = new ArrayList<String>();

        //limit the number of output list and only take the name instead of image path
        int resultNum = Math.min(rankedList.size(), Common.MAX_RESULTS);
        for(int i = 0; i < resultNum; i++){
            String[] imagePathTerms = rankedList.get(i).split("/");
            String imageID =imagePathTerms[imagePathTerms.length-1].trim();
            rankedFilteredList.add(imageID.substring(0, imageID.length() - 4));
        }
        return rankedFilteredList;
    }

    public double[] getRelevanceScore(String image1, List<String> rankedList){
        int accumulatedScore = 0;
        for(int i = 0; i < rankedList.size(); i++){
            String image2 = rankedList.get(i);
            boolean isRelevant = isRelevant(image1, image2);
            if(isRelevant){
                accumulatedScore++;
            }
        }
        double[] same_total = {(double) accumulatedScore, (double) rankedList.size(), 40.0};
        return same_total;
    }

    public boolean isRelevant(String image1, String image2){
        ArrayList<String> tagList1 = testGtmap.get(image1);
        ArrayList<String> tagList2 = trainGtmap.get(image2);
        for(int i = 0; i < tagList1.size(); i++){
            for(int j = 0; j < tagList2.size(); j++){
                if(!tagList1.get(i).equals("") &&
                        !tagList2.get(j).equals("") &&
                        tagList1.get(i).equals(tagList2.get(j))){
                    return true;
                }
            }
        }
        return false;
    }
}

