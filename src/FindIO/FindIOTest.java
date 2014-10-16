package FindIO;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Beyond on 10/16/2014 0016.
 */
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
        }

        System.out.println("Number of query images: " + count);
        reader.close();
    }

    public void evaluateSearch(String benchmarkFile) throws IOException{
        FileWriter fileWriter = new FileWriter(new File(benchmarkFile));
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("imageName\tch\tvw\tchvw\tvc\tchvc\tvwvc\tchvwvc");

        for(String imageName : testGtmap.keySet()){
            boolean isCHSelected, isVWSelected, isVCSelected, isTextSelected;
            stringBuilder.append(imageName+"\t");
            for(int i = 1; i <= 7; i++){
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
                List<String> rankedList = searchFile(imageName, isCHSelected, isVWSelected, isVCSelected, isTextSelected);
                int[] precision = accumulateRelevanceScore(imageName, rankedList);
                stringBuilder.append(precision[0]+"/"+precision[1]+"\t");
                fileWriter.write(precision[0] + "/" + precision[1] + "\t");
                System.out.print("CH " + isCHSelected + "  VW " + isVWSelected + "  VC " + isVCSelected);
                System.out.println(precision[0] + "/" + precision[3] + " " + precision[1] + "/" + precision[3] + " " + precision[2] + "/" + precision[3]);
            }
            stringBuilder.append(String.format("%n"));
            System.out.println();
            fileWriter.write(String.format("%n"));
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

    public int[] accumulateRelevanceScore(String image1, List<String> rankedList){
        int accumulatedScore = 0;
        int weightedScore = 0;
        int topScore = 0;
        for(int i = 0; i < rankedList.size(); i++){
            String image2 = rankedList.get(i);
            int score = calculateRelevanceScore(image1, image2);
            if(i <= 5){
                accumulatedScore += score;
                weightedScore += 3*score;
                topScore += score;
            } else if(i <=10){
                accumulatedScore += score;
                weightedScore += 2*score;
            } else {
                accumulatedScore += score;
                weightedScore += score;
            }
        }
        int[] same_total = {accumulatedScore, weightedScore, topScore, rankedList.size()};
        return same_total;
    }

    public int calculateRelevanceScore(String image1, String image2){
        ArrayList<String> tagList1 = testGtmap.get(image1);
        ArrayList<String> tagList2 = trainGtmap.get(image2);
        int score = getArraySimilarity(tagList1, tagList2);
        if(score>=1) {
            System.out.println(image1 + " " + image2 + "\t" + score);
        }
        return score;
    }

    public int getArraySimilarity(ArrayList<String> list1, ArrayList<String> list2){
        int numSharedTags = 0;
        for(int i = 0; i < list1.size(); i++){
            for(int j = 0; j < list2.size(); j++){
                if(!list1.get(i).equals("") &&
                    !list2.get(j).equals("") &&
                     list1.get(i).equals(list2.get(j))){
                    numSharedTags++;
                }
            }
        }
        return numSharedTags;
    }


}

