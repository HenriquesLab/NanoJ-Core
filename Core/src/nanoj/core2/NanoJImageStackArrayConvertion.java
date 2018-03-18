package nanoj.core2;

import ij.IJ;
import ij.ImageStack;
import ij.process.FloatProcessor;

public class NanoJImageStackArrayConvertion {

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
}
