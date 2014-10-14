package FindIO;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ColorHistExtraction {

	private static final int dim = 64;

    /* Get color histogram function */
	public static double[] getHist(File file) throws Exception {
        BufferedImage image = readImage(file);
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

    private static BufferedImage readImage(File file) throws IOException
    {
        return readImage(new FileInputStream(file));
    }

    private static BufferedImage readImage(InputStream stream) throws IOException
    {
        Iterator<ImageReader> imageReaders =
                ImageIO.getImageReadersBySuffix("jpg");
        ImageReader imageReader = imageReaders.next();
        ImageInputStream iis =
                ImageIO.createImageInputStream(stream);
        imageReader.setInput(iis, true, true);
        Raster raster = imageReader.readRaster(0, null);
        int w = raster.getWidth();
        int h = raster.getHeight();

        BufferedImage result =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int rgb[] = new int[3];
        int pixel[] = new int[3];
        for (int x=0; x<w; x++)
        {
            for (int y=0; y<h; y++)
            {
                raster.getPixel(x, y, pixel);
                int Y = pixel[0];
                int CR = pixel[1];
                int CB = pixel[2];
                toRGB(Y, CB, CR, rgb);
                int r = rgb[0];
                int g = rgb[1];
                int b = rgb[2];
                int bgr =
                        ((b & 0xFF) << 16) |
                                ((g & 0xFF) <<  8) |
                                (r & 0xFF);
                result.setRGB(x, y, bgr);
            }
        }
        return result;
    }

    // Based on http://www.equasys.de/colorconversion.html
    private static void toRGB(int y, int cb, int cr, int rgb[])
    {
        float Y = y / 255.0f;
        float Cb = (cb-128) / 255.0f;
        float Cr = (cr-128) / 255.0f;

        float R = Y + 1.4f * Cr;
        float G = Y -0.343f * Cb - 0.711f * Cr;
        float B = Y + 1.765f * Cb;

        R = Math.min(1.0f, Math.max(0.0f, R));
        G = Math.min(1.0f, Math.max(0.0f, G));
        B = Math.min(1.0f, Math.max(0.0f, B));

        int r = (int)(R * 255);
        int g = (int)(G * 255);
        int b = (int)(B * 255);

        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;
    }

    public static double[] getDefaultColorHist() {
        return new double[dim * dim * dim];
    }

    /* Calculate similarity between 2 color histograms */
    public static double calculateSimilarity(double[] array1, double[] array2){

        return 1 - calculateDistance(array1, array2);
    }

    /* Calculate difference between 2 color histograms */
	public static double calculateDistance(double[] array1, double[] array2){
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
        String sampleImgPath = "./src/FindIO/Datasets/train/data/tower/0097_159739573.jpg";
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
