package nanoj.core.java.array;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import static java.lang.Math.min;

/**
 *
 * ImageJ data type to/from primitive array conversions
 *
 * @author Henriques Lab
 *
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 3/29/13
 * Time: 1:16 PM
 */
//TODO: exceptions

public class ImageStackToFromArray {

    /**
     * Convert ImagePlus to 1d float array
     * @param imp
     * @return
     */
    public static float[] ImagePlusToFloatArray(ImagePlus imp) {
        return ImageStackToFloatArray(imp.getStack());
    }

    /**
     * Convert ImageStack to 1d float array
     * @param ims
     * @return
     */
    public static float[] ImageStackToFloatArray(ImageStack ims) {
        return ImageStackToFloatArray(ims, 1, ims.getSize());
    }

    /**
     * Convert frames from nstart to nstop of ImageStack to 1d float array
     * @param ims
     * @param nstart
     * @param nstop
     * @return
     */
    public static float[] ImageStackToFloatArray(ImageStack ims, int nstart, int nstop) {
        int width = ims.getWidth();
        int height = ims.getHeight();
        int widthHeight = width * height;

        int depth = (nstop - nstart + 1);
        long npixels = widthHeight * depth;

        if (npixels >= Integer.MAX_VALUE){
            IJ.error("Array too big for 1d conversion");
            assert (npixels < Integer.MAX_VALUE);
        }

        if (nstart == nstop) {
            return (float[]) ims.getProcessor(nstart).convertToFloat().getPixels();
        }

        float[] pixelsStack = new float[(int)npixels];
        float[] pixelsFrame;
        for (int n = 0; n < depth; n++) {
            pixelsFrame = (float[]) ims.getProcessor(n + nstart).convertToFloat().getPixels();
            System.arraycopy(pixelsFrame, 0, pixelsStack, n * widthHeight, widthHeight);
        }
        return pixelsStack;
    }

    /**
     * Convert pixels from (xstart,ystart) to (xend,yend) in frames from nstart to nstop of ImageStack to 1d float array
     * @param ims
     * @param xstart
     * @param xend
     * @param ystart
     * @param yend
     * @param nstart
     * @param nstop
     * @return
     */
    public static float[] ImageStackToFloatArray(ImageStack ims,
                                                 int xstart, int xend,
                                                 int ystart, int yend,
                                                 int nstart, int nstop) {

        nstop = min(nstop, ims.getSize());

        int depth = (nstop - nstart + 1);
        int subWidth = (xend - xstart + 1);
        int subHeight = (yend - ystart + 1);
        long npixels = subWidth * subHeight * depth;


        if (npixels >= Integer.MAX_VALUE){
            IJ.error("Array too big for 1d conversion");
            assert (npixels < Integer.MAX_VALUE);
        }

        float[] pixels = new float[(int)npixels];

        ImageProcessor ip;
        int counter = 0;
        for (int n = 0; n < depth; n++) {
            ip = ims.getProcessor(n + nstart);

            for (int y = 0; y < subHeight; y++) {
                for (int x = 0; x < subWidth; x++) {
                    pixels[counter] = ip.getf(xstart + x, ystart + y);
                    counter++;
                }
            }
        }
        return pixels;
    }

    /**
     * Convert 1d float array to ImageStack of dimensions width, height
     * @param pixelsStack
     * @param width
     * @param height
     * @return
     */
    public static ImageStack ImageStackFromFloatArray(float[] pixelsStack, int width, int height) {
        int widthHeight = width * height;
        int depth = pixelsStack.length / widthHeight;
        assert (pixelsStack.length/widthHeight == 0);

        ImageStack ims = new ImageStack(width, height);

        float[] pixelsFrame;
        for (int n = 0; n < depth; n++) {
            pixelsFrame = new float[widthHeight];
            System.arraycopy(pixelsStack, n * widthHeight, pixelsFrame, 0, widthHeight);
            ims.addSlice(new FloatProcessor(width, height, pixelsFrame));
        }
        return ims;
    }

    /**
     * Convert 1d short array to ImageStack of dimensions width, height
     * @param pixelsStack
     * @param width
     * @param height
     * @return
     */
    public static ImageStack ImageStackFromShortArray(short[] pixelsStack, int width, int height) {
        int widthHeight = width * height;
        int depth = pixelsStack.length / widthHeight;
        assert (pixelsStack.length/widthHeight == 0);

        ImageStack ims = new ImageStack(width, height);

        short[] pixelsFrame;
        for (int n = 0; n < depth; n++) {
            pixelsFrame = new short[widthHeight];
            System.arraycopy(pixelsStack, n * widthHeight, pixelsFrame, 0, widthHeight);
            ims.addSlice(new ShortProcessor(width, height, pixelsFrame, null));
        }
        return ims;
    }

    /**
     * Convert 1d float array to new 32bit ImagePlus of dimensions width, height
     * @param pixels
     * @param width
     * @param height
     * @return
     */
    public static ImagePlus ImagePlusFromFloatArray(float[] pixels, int width, int height) {
        return new ImagePlus("New 32-bit image", ImageStackFromFloatArray(pixels, width, height));
    }

    /**
     * Convert 3d float array to ImageStack of dimensions frames, width, height
     * @param inputArray
     * @param nOfFrames
     * @param outputImpWidth
     * @param outputImpHeight
     * @return
     */
    public static ImageStack MakeImsFrom3DArray(float[][][] inputArray, int nOfFrames, int outputImpWidth, int outputImpHeight) {
        //TODO: get array size for dimensions
        ImageStack ims = new ImageStack(outputImpWidth, outputImpHeight);
        FloatProcessor ip;

        for (int outputFrameN = 0; outputFrameN < nOfFrames; outputFrameN++) {
            ip = new FloatProcessor(outputImpWidth, outputImpHeight);
            for (int y = 0; y < outputImpHeight; y++) {
                for (int x = 0; x < outputImpWidth; x++) {
                    ip.setf(x, y, inputArray[outputFrameN][x][y]);
                    // TODO: consider doing per line System.arraycopy for speedup
                }
            }
            ims.addSlice(ip);
        }

        return ims;
    }
}
