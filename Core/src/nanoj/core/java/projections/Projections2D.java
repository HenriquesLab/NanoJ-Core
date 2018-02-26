package nanoj.core.java.projections;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;

import static nanoj.core.java.array.ImageStackToFromArray.ImageStackFromFloatArray;
import static nanoj.core.java.array.ImageStackToFromArray.ImageStackToFloatArray;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 15/02/15
 * Time: 14:33
 */
public class Projections2D {

    public static final int SUM = Kernel_group2DProject.SUM;
    public static final int AVERAGE = Kernel_group2DProject.AVERAGE;
    public static final int VARIANCE = Kernel_group2DProject.VARIANCE;
    public static final int STDDEV = Kernel_group2DProject.STDDEV;
    public static final int AVERAGE_IGNORING_ZEROS = Kernel_group2DProject.AVERAGE_IGNORING_ZEROS;
    public static final int STDDEV_IGNORING_ZEROS = Kernel_group2DProject.STDDEV_IGNORING_ZEROS;
    public static final int MAX = Kernel_group2DProject.MAX;
    public static final int MIN = Kernel_group2DProject.MIN;

    private static Kernel_group2DProject kernel_group2DProject = new Kernel_group2DProject();

    public static FloatProcessor averageProjection(ImageStack ims) {
        return (FloatProcessor) do2DProjection(ims, ims.getSize(), false, AVERAGE).getProcessor(1);
    }

    public static FloatProcessor weightedAverageProjection(ImageStack ims, float[] weights) {
        kernel_group2DProject.setWeights(weights);
        FloatProcessor fp = (FloatProcessor) do2DProjection(ims, ims.getSize(), false, AVERAGE).getProcessor(1);
        kernel_group2DProject.setNotWeighted();
        return fp;
    }

    public static FloatProcessor stdDevIgnoringZerosProjection(ImageStack ims) {
        FloatProcessor fp = (FloatProcessor) do2DProjection(ims, ims.getSize(), false, STDDEV_IGNORING_ZEROS).getProcessor(1);
        return fp;
    }

    public static FloatProcessor do2DProjection(ImageStack ims, int projectionType) {
        return (FloatProcessor) do2DProjection(ims, ims.getSize(), false, projectionType, 0, 0, 0, 0).getProcessor(1);
    }

    public static ImageStack do2DProjection(ImageStack ims, int framesPerGroup,
                                            boolean useSlidingWindow, int projectionType) {
        return do2DProjection(ims, framesPerGroup, useSlidingWindow, projectionType, 0, 0, 0, 0);
    }

    public static ImageStack do2DProjection(ImageStack ims, int framesPerGroup,
                                            boolean useSlidingWindow, int projectionType,
                                            int xMargin0, int xMargin1, int yMargin0, int yMargin1) {
        int width = ims.getWidth();
        int height = ims.getHeight();
        int interiorWidth = width - xMargin0 - xMargin1;
        int interiorHeight = height - yMargin0 - yMargin1;

        float[] pixels = ImageStackToFloatArray(ims);
        pixels = kernel_group2DProject.doProjection(pixels, width, height,
                framesPerGroup, useSlidingWindow, projectionType,
                xMargin0, xMargin1, yMargin0, yMargin1, interiorWidth, interiorHeight);
        return ImageStackFromFloatArray(pixels, interiorWidth, interiorHeight);
    }
}

class Kernel_group2DProject extends NJKernel {
    public static final int SUM = 0;
    public static final int AVERAGE = 1;
    public static final int VARIANCE = 2;
    public static final int STDDEV = 3;
    public static final int AVERAGE_IGNORING_ZEROS = 4;
    public static final int STDDEV_IGNORING_ZEROS = 5;
    public static final int MAX = 6;
    public static final int MIN = 7;

    private float[] pixelsOriginal, pixelsProjection, weights;
    private int mode, framesPerGroup, nGroups, nFramesOriginal,
            width, height, widthHeight, xMargin0, xMargin1, yMargin0, yMargin1,
            interiorWidth, interiorHeight, interiorWidthHeight;

    private int useSlidingWindow;
    private int isWeighted = 0;

    public void setWeights(float[] weights) {
        this.weights = weights;
        isWeighted = 1;
    }

    public void setNotWeighted() {
        this.weights = null;
        isWeighted = 0;
    }

    public float[] doProjection(float[] pixels, int width, int height, int framesPerGroup,
                                boolean useSlidingWindow, int projectionType,
                                int xMargin0, int xMargin1, int yMargin0, int yMargin1,
                                int interiorWidth, int interiorHeight) {

        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.xMargin0 = xMargin0;
        this.xMargin1 = xMargin1;
        this.yMargin0 = yMargin0;
        this.yMargin1 = yMargin1;
        this.interiorWidth = interiorWidth;
        this.interiorHeight = interiorHeight;
        this.interiorWidthHeight = interiorWidth * interiorHeight;

        this.useSlidingWindow = useSlidingWindow? 1: 0;
        this.mode = projectionType;

        this.framesPerGroup = framesPerGroup;
        this.nFramesOriginal = pixels.length / widthHeight;

        if (weights == null || projectionType != AVERAGE) {
            weights = new float[] {0};
            isWeighted = 0;
        }
        else assert (weights.length == nFramesOriginal);

        if (!useSlidingWindow) {
            this.nGroups = nFramesOriginal / framesPerGroup;
            if (framesPerGroup * nGroups != nFramesOriginal) this.nGroups++;
        }
        else {
            this.nGroups = nFramesOriginal;
        }

        this.pixelsOriginal = pixels;
        this.pixelsProjection = new float[interiorWidthHeight * nGroups];

        setExplicit(true);
        long memNeeded = (pixelsOriginal.length + pixelsProjection.length) * 32;
        if (memNeeded > 750000000) setExecutionMode(EXECUTION_MODE.JTP);
        else autoChooseDeviceForNanoJ();

        put(this.pixelsOriginal);
        put(this.pixelsProjection);
        put(this.weights);

        execute(this.pixelsProjection.length);

        get(this.pixelsProjection);
        return this.pixelsProjection;
    }

    @Override
    public void run() {
        int p, x, y, g;

        p = getGlobalId(0);
        x = p % (interiorWidth);
        y = (p / interiorWidth) % interiorHeight;
        g = p / (interiorWidthHeight);

        int tStart = 0;
        int tEnd = 0;

        if (this.useSlidingWindow == 0) {
            tStart = g*framesPerGroup;
            tEnd = min(tStart + framesPerGroup, nFramesOriginal);
        }
        else {
            tStart = max(g - framesPerGroup/2, 0);
            tEnd = min(g + framesPerGroup/2, nFramesOriginal);
        }
        int nTsInGroup = tEnd-tStart;

        if (mode == SUM)
            pixelsProjection[getIdxP(x, y, g)] = getAverage(x, y, tStart, tEnd) * nTsInGroup;
        else if (mode == AVERAGE) {
            if (isWeighted == 0) pixelsProjection[getIdxP(x, y, g)] = getAverage(x, y, tStart, tEnd);
            else pixelsProjection[getIdxP(x, y, g)] = getWeightedAverage(x, y, tStart, tEnd);
        }
        else if (mode == VARIANCE)
            pixelsProjection[getIdxP(x, y, g)] = getVariance(x, y, tStart, tEnd);
        else if (mode == STDDEV)
            pixelsProjection[getIdxP(x, y, g)] = sqrt(getVariance(x, y, tStart, tEnd));
        else if (mode == STDDEV_IGNORING_ZEROS)
            pixelsProjection[getIdxP(x, y, g)] = sqrt(getVarianceIgnoringZeros(x, y, tStart, tEnd));
        else if (mode == MAX)
            pixelsProjection[getIdxP(x, y, g)] = getMax(x, y, tStart, tEnd);
        else if (mode == MIN)
            pixelsProjection[getIdxP(x, y, g)] = getMin(x, y, tStart, tEnd);
    }

    private float getAverage(int x, int y, int tStart, int tEnd) {
        int nTsInGroup = tEnd-tStart;
        float v = 0;
        for (int t=tStart; t<tEnd; t++) {
            v += pixelsOriginal[getIdxO(x, y, t)] / nTsInGroup;
        }
        return v;
    }

    private float getWeightedAverage(int x, int y, int tStart, int tEnd) {
        float weightSum = 0;
        float v = 0;
        for (int t=tStart; t<tEnd; t++) {
            v += pixelsOriginal[getIdxO(x, y, t)] * weights[t];
            weightSum += weights[t];
        }
        if (weightSum != 0) v /= weightSum;
        else v = 0;
        return v;
    }

    private float getAverageIgnoringZeros(int x, int y, int tStart, int tEnd) {
        float v = 0, mean = 0;
        int counter = 0;
        for (int t=tStart; t<tEnd; t++) {
            v = pixelsOriginal[getIdxO(x, y, t)];
            if (v!=0) {
                counter++;
                mean += (v - mean) / counter;
            }
        }
        return mean;
    }

    private float getVariance(int x, int y, int tStart, int tEnd) {
        int nTsInGroup = tEnd-tStart;
        float mean = getAverage(x, y, tStart, tEnd);
        float v = 0;
        for (int t = tStart; t < tEnd; t++) {
            v += pow(pixelsOriginal[getIdxO(x, y, t)] - mean, 2) / nTsInGroup;
        }
        return v;
    }

    private float getVarianceIgnoringZeros(int x, int y, int tStart, int tEnd) {
        float mean = getAverageIgnoringZeros(x, y, tStart, tEnd);
        float v = 0, var = 0;
        int counter = 0;
        for (int t = tStart; t < tEnd; t++) {
            v = pixelsOriginal[getIdxO(x, y, t)];
            if (v!=0) {
                var += pow(pixelsOriginal[getIdxO(x, y, t)] - mean, 2);
                counter++;
            }
        }
        if (counter == 0) return 0;
        return var/counter;
    }

    private float getMax(int x, int y, int tStart, int tEnd){

        float v=0;
        for (int t=tStart; t<tEnd; t++) {
             if(pixelsOriginal[getIdxO(x, y, t)]>v){
                 v=pixelsOriginal[getIdxO(x, y, t)];
             }
        }
        return v;
    }

    private float getMin(int x, int y, int tStart, int tEnd){

        float v=Float.MAX_VALUE;
        for (int t=tStart; t<tEnd; t++) {
            if(pixelsOriginal[getIdxO(x, y, t)]<v){
                v=pixelsOriginal[getIdxO(x, y, t)];
            }
        }
        return v;
    }

    private int getIdxP(int x, int y, int g) { // idx in projection
        int pg = g * interiorWidthHeight; // position in time * pixels in a frame
        int pf = y * interiorWidth + x; // position within a frame
        return pg + pf;
    }

    private int getIdxO(int x, int y, int t) { // idx in original
        x = min(max(0, x + xMargin0), width - 1);
        y = min(max(0, y + yMargin0), height-1);
        int pt = t * widthHeight; // position in time * pixels in a frame
        int pf = y * width + x; // position within a frame
        return pt + pf;
    }
}
