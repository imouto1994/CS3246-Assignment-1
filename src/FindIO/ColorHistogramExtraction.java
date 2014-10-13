package FindIO;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ColorHistogramExtraction {

	private static final int dim = 64;


    /* Get color histogram function */
	public static double[] getHist(File file) throws Throwable {
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        BufferedImage image = null;
        Exception lastException = null;
        while (iter.hasNext()) {
            ImageReader reader = null;
            try {
                reader = (ImageReader)iter.next();
                ImageReadParam param = reader.getDefaultReadParam();
                reader.setInput(stream, true, true);
                Iterator<ImageTypeSpecifier> imageTypes = reader.getImageTypes(0);
                while (imageTypes.hasNext()) {
                    ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
                    int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
                    if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY) {
                        param.setDestinationType(imageTypeSpecifier);
                        break;
                    }
                }
                image = reader.read(0, param);
                if (image != null) break;
            } catch (Exception e) {
                lastException = e;
            } finally {
                if (reader != null)
                    reader.dispose();
            }
        }
        // If you don't have an image at the end of all readers
        if (image == null) {
            if (null != lastException) {
                throw lastException;
            }
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

                //Changed Codes.
                int y  = (int)( 0 + 0.299   * r + 0.587   * g + 0.114   * b);
                int cb = (int)(128 -0.16874 * r - 0.33126 * g + 0.50000 * b);
                int cr = (int)(128 + 0.50000 * r - 0.41869 * g - 0.08131 * b);

                int ybin = y / step;
                int cbbin = cb / step;
                int crbin = cr / step;

                //Changed Codes.
                bins[ybin*dim*dim+cbbin*dim+crbin] ++;
            }
        }

        //Changed Codes.
        for(int i = 0; i < dim*dim*dim; i++) {
            bins[i] = bins[i]/(imHeight*imWidth);
        }

        return bins;
	}

    public static double[] getDefaultColorHist() {
        return new double[dim * dim * dim];
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
