package FindIO;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import java.io.*;
import java.util.Arrays;

public class VisualWordExtraction {

    public static void main(String[] args){
        getVisualWords(null);
    }

    public static double[] getVisualWords(File file){
        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        String fileNameWithoutExtension = fileName.replace("." + fileExtension, "");
        String fileParentDirectory = file.getAbsoluteFile().getParent();
        System.out.println(fileParentDirectory);
        String workingDirectory = System.getProperty("user.dir");
        String featureDirectory = workingDirectory + "\\src\\FindIO\\Features\\Visual Word\\ScSPM\\";

        // Create Proxy
        MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                .setUsePreviouslyControlledSession(true)
                .setHidden(true)
                .build();
        MatlabProxyFactory factory = new MatlabProxyFactory(options);
        MatlabProxy proxy = null;
        try {
            proxy = factory.getProxy();
            proxy.eval("addpath('" + featureDirectory + "')");
            proxy.eval("cd('" + featureDirectory + "')");
            proxy.feval("generateSparsefeature", fileParentDirectory, Boolean.FALSE,".\\siftfeature", ".\\siftpooling");
            proxy.eval("rmpath('" + featureDirectory + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }

        File resultFile = new File(featureDirectory + "siftpooling\\" + fileNameWithoutExtension);
        if(!resultFile.exists()){
            System.out.println("Result file was not created");
        }

        double[] words = new double[21504];
        try {
            BufferedReader br = new BufferedReader(new FileReader(resultFile.getAbsolutePath()));
            String line;
            while((line = br.readLine()) != null){
                String[] freqs = line.trim().split("\\s+");
                words = new double[freqs.length];
                for(int i = 0; i < freqs.length; i++){
                    words[i] = Double.parseDouble(freqs[i]);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Result file is not created");
        } catch (IOException e) {
            System.out.println("Cannot read line from results");
        }

        resultFile.delete();

        return words;
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
