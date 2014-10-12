package FindIO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

public class ColorHistogramExtraction {

	private static final int dim = 64;


    /* Get color histogram function */
	public static double[] getHist(File file) throws Throwable {

        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("error when reading image "+file.getName());
            e.printStackTrace();
        }
        int imHeight = image.getHeight();
        int imWidth = image.getWidth();
        double[] bins = new double[dim*dim*dim];
        int step = 256 / dim;
        Raster raster = image.getRaster();
        for(int i = 0; i < imWidth; i++)
        {
            for(int j = 0; j < imHeight; j++)
            {
            	// rgb->ycrcb
            	int r = raster.getSample(i,j,0);
            	int g = raster.getSample(i,j,1);
            	int b = raster.getSample(i,j,2);
            	int y  = (int)( 0.299   * r + 0.587   * g + 0.114   * b);
        		int cb = (int)(-0.16874 * r - 0.33126 * g + 0.50000 * b);
        		int cr = (int)( 0.50000 * r - 0.41869 * g - 0.08131 * b);
        		
        		int ybin = y / step;
        		int cbbin = cb / step;
        		int crbin = cr / step;

                bins[ ybin*dim*dim+cbbin*dim+crbin ] ++;

            }
        }
        
        //normalize
        for(int i = 0; i < 3*dim; i++) {
        	bins[i] = bins[i]/(imHeight*imWidth);
        }
        
        return bins;
	}

    /* Calculate similarity between 2 color histograms */
    public static double calculateSimilarity(double[] array1, double[] array2){

        return 1 - calculateDistance(array1, array2);
    }

    /* Calculate difference between 2 color histograms */
	public static double calculateDistance(double[] array1, double[] array2)
    {
		// Euclidean distance
        /*double Sum = 0.0;
        for(int i = 0; i < array1.length; i++) {
           Sum = Sum + Math.pow((array1[i]-array2[i]),2.0);
        }
        return Math.sqrt(Sum);
        */
        
        // Bhattacharyya distance
		double h1 = 0.0;
		double h2 = 0.0;
		int N = array1.length;
        for(int i = 0; i < N; i++) {
        	h1 = h1 + array1[i];
        	h2 = h2 + array2[i];
        }

        double Sum = 0.0;
        for(int i = 0; i < N; i++) {
           Sum = Sum + Math.sqrt(array1[i]*array2[i]);
        }
        double dist = Math.sqrt( 1 - Sum / Math.sqrt(h1*h2));
        return dist;
    }

    //Test the main funciton
    public static void main(String[] args){
        String sampleImgPath = "./src/FindIO/Datasets/train/data/bear/0018_167630455.jpg";
        File img = new File(sampleImgPath);
        if(img.exists() && !img.isDirectory()){
            try {
                double[] colorHist = getHist(img);
                int count = 0;
                for(int i = 0; i < colorHist.length; i++){
                    double frequency = colorHist[i];
                    if(frequency >= 1){
                        System.out.print(i+" "+frequency+"  ");
                        count++;
                    }
                }
                System.out.println("\nTotal number of useful bins: "+count+"\tin all bins: "+dim*dim*dim);
            } catch(Throwable e){
                e.printStackTrace();
            }

        } else {
            System.out.println(Common.MESSAGE_FILE_NOTEXIST);
        }

    }
}
