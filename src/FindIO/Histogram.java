package FindIO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Histogram {
    private int SIZE = 256;
    // Red, Green, Blue   
    private int NUMBER_OF_COLOURS = 3;

    public final int RED = 0;
    public final int GREEN = 1;
    public final int BLUE = 2;

    private int[][] colourBins;
    private volatile boolean loaded = false;
    private int maxY;

    /**
     * 
     * @param Path
     *            of image to create Histogram of.
     */
    public Histogram() {
        colourBins = new int[NUMBER_OF_COLOURS][];

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            colourBins[i] = new int[SIZE];
        }

        loaded = false;
    }

    public void load(String path) throws IOException {
        BufferedImage bi = ImageIO.read(new File(path));

            // Reset all the bins
        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
           for (int j = 0; j < SIZE; j++) {
                colourBins[i][j] = 0;
           }
        }

        for (int x = 0; x < bi.getWidth(); x++) {
            for (int y = 0; y < bi.getHeight(); y++) {
                Color c = new Color(bi.getRGB(x, y));

                colourBins[RED][c.getRed()]++;
                colourBins[GREEN][c.getGreen()]++;
                colourBins[BLUE][c.getBlue()]++;
            }
        }

        maxY = 0;

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (maxY < colourBins[i][j]) {
                    maxY = colourBins[i][j];
                }
            }
        }

        loaded = true;
    }
}
