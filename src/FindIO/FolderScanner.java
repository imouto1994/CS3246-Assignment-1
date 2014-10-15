package FindIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileWriter;

public class FolderScanner {

    String dataFolder;
    String gtFile;

    HashMap<String, ArrayList<String>> imgFolderMap;

    public FolderScanner(String dataFolder, String gtFile){
        this.dataFolder = dataFolder;
        this.gtFile = gtFile;
    }

    public void scanTrainSetFolder(){
        imgFolderMap = new HashMap<String, ArrayList<String>>();

        File file = new File(this.dataFolder);
        File[] imageFolders = file.listFiles();
        for(int i = 0; i < imageFolders.length; i++) {
            File folder = imageFolders[i];
            if(folder.exists() && folder.isDirectory()){
                File[] images = folder.listFiles();
                for(File image : images){
                    if(image.exists() && !image.isDirectory()){

                        //add the image name and containing folder name to the hashmap
                        String imageName = image.getName();
                        if(imgFolderMap.containsKey(imageName)){
                            ArrayList<String> folders = imgFolderMap.get(imageName);
                            folders.add(folder.getName());
                            imgFolderMap.put(imageName, folders);
                        } else {
                            ArrayList<String> folders = new ArrayList<String>();
                            folders.add(folder.getName());
                            imgFolderMap.put(imageName, folders);
                        }
                    }
                }
            }
        }
    }

    public void writeToGtFile() throws IOException{
        FileWriter fileWriter = new FileWriter(new File(this.gtFile));
        StringBuffer strbuf = new StringBuffer();
        for(String imageName : imgFolderMap.keySet()){
            ArrayList<String> folders = imgFolderMap.get(imageName);

            strbuf.append(imageName+"  ");
            for(String folderName : folders) {
                strbuf.append(folderName+"  ");
            }
            strbuf.append(String.format("%n"));
        }
        fileWriter.write(strbuf.toString());
        fileWriter.close();
    }

    public static void main(String[] args){
        String trainFolder = "./src/FindIO/Datasets/test/query";
        String gtFile = "./src/FindIO/query_groundTruth.txt";
        FolderScanner folderScanner = new FolderScanner(trainFolder, gtFile);
        folderScanner.scanTrainSetFolder();
        try{
            folderScanner.writeToGtFile();
        } catch (IOException e) {
            System.out.println("Error occurs when writing to ground truth file");
            e.printStackTrace();
        }
    }

}
