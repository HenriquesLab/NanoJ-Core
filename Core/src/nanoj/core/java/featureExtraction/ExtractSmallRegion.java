package nanoj.core.java.featureExtraction;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Created by Henriques-lab on 20/11/2016.
 */
public class ExtractSmallRegion {

    public static ImageProcessor extractSmall2DRegion(ImageProcessor ip, int width, int height) {
        int w = ip.getWidth();
        int h = ip.getHeight();

        if (w > width || h > height) {
            ip.setRoi((int) (w/2f-width/2f), (int) (h/2f-height/2f), width, height);
            return ip.crop();
        }
        return ip;
    }

    public static ImageProcessor extractSmall2DRegion(ImagePlus imp, int width, int height) {
        FloatProcessor ip = imp.getProcessor().convertToFloatProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        if (imp.getRoi() != null) {
            ip.setRoi(imp.getRoi());
            return  ip.crop();
        }
        return extractSmall2DRegion(ip, width, height);
    }
}
