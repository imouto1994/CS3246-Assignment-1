package FindIO;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import java.io.File;

public class VisualWordExtraction {

    public static void main(String[] args){

    }

    public static double[] getVisualConcepts(File file){
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
            // Call Built-In Function
            proxy.eval("addpath('" + featureDirectory + "')");
            proxy.eval("cd('" + featureDirectory + "')");
            proxy.feval("generateSparsefeature",".\\Demo_test1", "true", ".\\siftfeature1",".\\siftpooling1");
            proxy.eval("rmpath('" + featureDirectory + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
