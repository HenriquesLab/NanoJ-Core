package nanoj.core.java.image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import static nanoj.core.java.array.ImageStackToFromArray.ImageStackFromFloatArray;
import static nanoj.core.java.array.ImageStackToFromArray.ImageStackToFloatArray;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class ResizeImage {
    public final static int NEAREST_NEIGHBOUR = 0;
    public final static int BILINEAR = 1;
    public final static int BICUBIC = 2;
    private static int interpolationMethod = 2;

    private static Kernel_IncreaseSize kernelIncrease = new Kernel_IncreaseSize();
    private static Kernel_DecreaseSize kernelDecrease = new Kernel_DecreaseSize();

    public static void setInterpolationMethod(int method){
        assert (method>0);
        interpolationMethod = method;
    }

    public static ImageStack increaseSize(ImageStack ims, float magnification){
        //NOTE: this has a bug that shifts sample
        int width = ims.getWidth();
        int height = ims.getHeight();
        float[] pixels = ImageStackToFloatArray(ims);
        float[] pixelsResized = increaseSize(pixels, width, height, magnification);
        return ImageStackFromFloatArray(pixelsResized, kernelIncrease.widthM, kernelIncrease.heightM);
    }

    public static FloatProcessor increaseSize(FloatProcessor fp, float magnification){
        //NOTE: this has a bug that shifts sample
        int width = fp.getWidth();
        int height = fp.getHeight();
        float[] pixels = (float[]) fp.getPixels();
        float[] pixelsResized = increaseSize(pixels, width, height, magnification);
        return new FloatProcessor(kernelIncrease.widthM, kernelIncrease.heightM, pixelsResized);
    }

    public static float[] increaseSize(float[] pixels, int width, int height, float magnification){
        //NOTE: this has a bug that shifts sample
        kernelIncrease.interpolationMethod = interpolationMethod;
        return kernelIncrease.calculate(pixels, width, height, magnification);
    }

    public static ImageStack decreaseSize(ImageStack ims, float shrinkSpace, float shrinkTime){
        int width = ims.getWidth();
        int height = ims.getHeight();
        float[] pixels = ImageStackToFloatArray(ims);
        float[] pixelsResized = decreaseSize(pixels, width, height, shrinkSpace, shrinkTime);
        return ImageStackFromFloatArray(pixelsResized, kernelDecrease.widthS, kernelDecrease.heightS);
    }

    public static FloatProcessor decreaseSize(FloatProcessor fp, float shrinkSpace, float shrinkTime) {
        int width = fp.getWidth();
        int height = fp.getHeight();
        float[] pixels = (float[]) fp.getPixels();
        float[] pixelsResized = decreaseSize(pixels, width, height, shrinkSpace, shrinkTime);
        return new FloatProcessor(kernelDecrease.widthS, kernelDecrease.heightS, pixelsResized);
    }

    public static float[] decreaseSize(float[] pixels, int width, int height, float shrinkSpace, float shrinkTime){
        return kernelDecrease.calculate(pixels, width, height, shrinkSpace, shrinkTime);
    }
}

class Kernel_IncreaseSize extends NJKernel {

    public final static int NEAREST_NEIGHBOUR = ResizeImage.NEAREST_NEIGHBOUR;
    public final static int BILINEAR = ResizeImage.BILINEAR;
    public final static int BICUBIC = ResizeImage.BICUBIC;

    private float[] pixelsOriginal, pixelsResized;
    private int width, height, widthHeight, nTimePoints;
    public int widthM, heightM, widthHeightM;
    public float magnification, magnificationX, magnificationY;

    public int interpolationMethod = BICUBIC;

    Log log = new Log();

    public float[] calculate(float[] pixels, int width, int height, float magnification) {

        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.nTimePoints = pixels.length/widthHeight;
        this.pixelsOriginal = pixels;

        this.widthM  = round(width * magnification);
        this.heightM = round(height * magnification);
        this.widthHeightM = widthM * heightM;
        this.pixelsResized = new float[this.widthHeightM*this.nTimePoints];

        this.magnification = magnification;
        this.magnificationX = ((float) widthM)/width;
        this.magnificationY = ((float) heightM)/height;

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixelsOriginal);
        put(this.pixelsResized);
        log.msg(3, "Kernel_IncreaseImageSize: resizing");
        execute(pixelsResized.length);
        log.msg(3, "Kernel_IncreaseImageSize: done");

        // Download arrays
        get(this.pixelsResized);
        return this.pixelsResized;
    }

    @Override public void run() {

        int p = getGlobalId();
        int x = p % (widthM);
        int y = (p / widthM) % heightM;
        int t = p / (widthHeightM);

        //float xCoarse = (x + 0.5f - 0.5f * magnificationX)/magnificationX;
        //float yCoarse = (y + 0.5f - 0.5f * magnificationY)/magnificationY;
        float xCoarse = (x + 0.5f)/magnificationX;
        float yCoarse = (y + 0.5f)/magnificationY;

        if (interpolationMethod == 0)
            pixelsResized[p] = getClosest(xCoarse, yCoarse, t);
        else if (interpolationMethod == 1)
            pixelsResized[p] = getBilinearInterpolatedPixel(xCoarse, yCoarse, t);
        else
            pixelsResized[p] = getBicubicInterpolatedPixel(xCoarse, yCoarse, t);
    }

    private int getIdx(int x, int y, int t){
        return t * widthHeight + y * width + x;
    }

    private float getClosest(float x, float y, int t) {
        x = x - 0.5f;
        y = y - 0.5f;
        int x_ = min(max((int)round(x), 0), width - 1);
        int y_ = min(max((int)round(y), 0), height - 1);
        return pixelsOriginal[getIdx(x_, y_, t)];
    }

    // Adapted from ImageJ's source
    private float getInterpolatedPixel(float x, float y, int t){
        if (x<0.5f || x>=width-0.5f || y<0.5f || y>=height-0.5f) {
            if (x<0.0f || x>=width || y<0.0f || y>=height)
                return 0;
            else
                return getInterpolatedEdgeValue(x, y, t);
        }
        int xbase = (int)(x-0.5f);
        int ybase = (int)(y-0.5f);
        float xFraction = x - (xbase+0.5f);
        float yFraction = y - (ybase+0.5f);

        if (xFraction < 0.0f) xFraction = 0.0f;
        if (yFraction < 0.0f) yFraction = 0.0f;

        float upperLeft = pixelsOriginal[getIdx(xbase, ybase, t)];
        float upperRight = pixelsOriginal[getIdx(xbase+1, ybase, t)];
        float lowerRight = pixelsOriginal[getIdx(xbase+1, ybase+1, t)];
        float lowerLeft = pixelsOriginal[getIdx(xbase, ybase+1, t)];
        float upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
        float lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);

        return upperAverage + yFraction * (lowerAverage - upperAverage);
    }

    // Adapted from ImageJ's source
    private float getBilinearInterpolatedPixel(float x, float y, int t) {
        if (x>0.0f && x<width && y>0.0f && y<height) {
            float value = getInterpolatedPixel(x, y, t);
            return value;
        } else
            return 0;
    }

    // Adapted from ImageJ's source
    public float getBicubicInterpolatedPixel(float x, float y, int t) {
        if (x<1.5f || x>width-1.5f || y<1.5f || y>height-1.5f)
            return getBilinearInterpolatedPixel(x, y, t);
        int u0 = (int) floor(x - 0.5f);
        int v0 = (int) floor(y - 0.5f);
        float q = 0.0f;
        for (int j = 0; j <= 3; j++) {
            int v = v0 - 1 + j;
            float p = 0.0f;
            for (int i = 0; i <= 3; i++) {
                int u = u0 - 1 + i;
                p = p + pixelsOriginal[getIdx(u,v,t)] * cubic(x - (u + 0.5f));
            }
            q = q + p * cubic(y - (v + 0.5f));
        }
        return q;
    }

    // Adapted from ImageJ's source
    private float cubic(float x) {
        float a = 0.5f; // Catmull-Rom interpolation
        if (x < 0.0f) x = -x;
        float z = 0.0f;
        if (x < 1.0f)
            z = x*x*(x*(-a+2.0f) + (a-3.0f)) + 1.0f;
        else if (x < 2.0f)
            z = -a*x*x*x + 5.0f*a*x*x - 8.0f*a*x + 4.0f*a;
        return z;
    }

    // Adapted from ImageJ's source
    private float getInterpolatedEdgeValue(float x, float y, int t) {
        int xbase = (int) floor(x-0.5f);
        int ybase = (int) floor(y-0.5f);
        float xFraction = x - (xbase+0.5f);
        float yFraction = y - (ybase+0.5f);
        if (xFraction<0) xFraction = xFraction*-1;
        if (yFraction<0) yFraction = yFraction*-1;
        float upperLeft = getEdgeValue(xbase, ybase, t);
        float upperRight = getEdgeValue(xbase+1, ybase, t);
        float lowerRight = getEdgeValue(xbase+1, ybase+1, t);
        float lowerLeft = getEdgeValue(xbase, ybase+1, t);
        float upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
        float lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);
        
        return upperAverage + yFraction * (lowerAverage - upperAverage);
    }

    // Adapted from ImageJ's source
    private float getEdgeValue(int x, int y, int t) {
        if (x==-1) x = 1;
        if (x==width) x = width-2;
        if (y==-1) y = 1;
        if (y==height) y = height-2;
        return pixelsOriginal[getIdx(x, y, t)];
    }

}

class Kernel_DecreaseSize extends NJKernel {
    private float[] pixelsOriginal, pixelsResized;
    private int width, height, widthHeight, nTimePoints;
    private float shrinkX, shrinkY, shrinkT;
    private int xRadius, yRadius, tRadius, pixelSum;
    public int widthS, heightS, widthHeightS, nTimePointsS;

    Log log = new Log();

    public float[] calculate(float[] pixels, int width, int height, float shrinkSpace, float shrinkTime) {
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.nTimePoints = pixels.length/widthHeight;
        this.pixelsOriginal = pixels;

        if (nTimePoints == 1) shrinkTime = 1;

        this.widthS = round(width / shrinkSpace);
        this.heightS = round(height / shrinkSpace);
        this.nTimePointsS = round(nTimePoints / shrinkTime);

        this.widthHeightS = widthS * heightS;
        this.pixelsResized = new float[this.widthHeightS *this.nTimePointsS];

        this.shrinkX = (float) width  / widthS;
        this.shrinkY = (float) height / heightS;
        this.shrinkT = (float) nTimePoints / nTimePointsS;

        this.xRadius = round((shrinkX-1) / 2);
        this.yRadius = round((shrinkY-1) / 2);
        this.tRadius = round((shrinkT-1) / 2);
        this.pixelSum = (2*tRadius+1)*(2*yRadius+1)*(2*xRadius+1);

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixelsOriginal);
        put(this.pixelsResized);
        log.msg(3, "Kernel_BinImage: resizing");
        execute(pixelsResized.length);
        log.msg(3, "Kernel_BinImage: done");

        // Download arrays
        get(this.pixelsResized);
        return this.pixelsResized;
    }

    @Override
    public void run() {
        int p = getGlobalId();
        int x = p % (widthS);
        int y = (p / widthS) % heightS;
        int t = p / (widthHeightS);
        float v = 0;

        float xM = x * shrinkX;
        float yM = y * shrinkY;
        float tM = t * shrinkT;

        for (int k = -tRadius; k <= tRadius; k++) {
            for (int j = -yRadius; j <= yRadius; j++) {
                for (int i = -xRadius; i <= xRadius; i++) {
                    v += getInterpolatedPixel(xM + i, yM + j, tM + k)/pixelSum;
                }
            }
        }
        pixelsResized[p] = v;
    }

    private int getIdx(int x, int y, int t){
        return t * widthHeight + y * width + x;
    }

    // Adapted from ImageJ's source
    private float getInterpolatedPixel(float x, float y, float t){
        if (x<0.0 || x>=width-1.0 || y<0.0 || y>=height-1.0 || t<0.0 || t>=nTimePoints-1.0) {
            if (x<-1.0 || x>=width || y<-1.0 || y>=height || t<-1.0 || t>=nTimePoints)
                return 0;
            else
                return getInterpolatedEdgeValue(x, y, t);
        }
        int xbase = (int)x;
        int ybase = (int)y;
        int tbase = (int)t;

        float xFraction = x - xbase;
        float yFraction = y - ybase;
        float tFraction = t - tbase;

        if (xFraction<0) xFraction = 0;
        if (yFraction<0) yFraction = 0;
        if (tFraction<0) tFraction = 0;

        float lowerLeftT0 = pixelsOriginal[getIdx(xbase, ybase, tbase)];
        float lowerRightT0 = pixelsOriginal[getIdx(xbase+1, ybase, tbase)];
        float upperRightT0 = pixelsOriginal[getIdx(xbase+1, ybase+1, tbase)];
        float upperLeftT0 = pixelsOriginal[getIdx(xbase, ybase+1, tbase)];

        float lowerLeftT1 = pixelsOriginal[getIdx(xbase, ybase, tbase+1)];
        float lowerRightT1 = pixelsOriginal[getIdx(xbase+1, ybase, tbase+1)];
        float upperRightT1 = pixelsOriginal[getIdx(xbase+1, ybase+1, tbase+1)];
        float upperLeftT1 = pixelsOriginal[getIdx(xbase, ybase+1, tbase+1)];

        float upperAverageT0 = upperLeftT0 + xFraction * (upperRightT0 - upperLeftT0);
        float lowerAverageT0 = lowerLeftT0 + xFraction * (lowerRightT0 - lowerLeftT0);
        float vT0 = lowerAverageT0 + yFraction * (upperAverageT0 - lowerAverageT0);

        float upperAverageT1 = upperLeftT1 + xFraction * (upperRightT1 - upperLeftT1);
        float lowerAverageT1 = lowerLeftT1 + xFraction * (lowerRightT1 - lowerLeftT1);
        float vT1 = lowerAverageT1 + yFraction * (upperAverageT1 - lowerAverageT1);

        return vT0 + tFraction * (vT1-vT0);
    }

    // Adapted from ImageJ's source
    private float getInterpolatedEdgeValue(float x, float y, float t) {
        int xbase = (int)x;
        int ybase = (int)y;
        int tbase = (int)t;

        float xFraction = x - xbase;
        float yFraction = y - ybase;
        float tFraction = t - tbase;

        if (xFraction<0) xFraction = 0;
        if (yFraction<0) yFraction = 0;
        if (tFraction<0) tFraction = 0;

        float lowerLeftT0 = getEdgeValue(xbase, ybase, tbase);
        float lowerRightT0 = getEdgeValue(xbase+1, ybase, tbase);
        float upperRightT0 = getEdgeValue(xbase+1, ybase+1, tbase);
        float upperLeftT0 = getEdgeValue(xbase, ybase+1, tbase);

        float lowerLeftT1 = getEdgeValue(xbase, ybase, tbase+1);
        float lowerRightT1 = getEdgeValue(xbase+1, ybase, tbase+1);
        float upperRightT1 = getEdgeValue(xbase+1, ybase+1, tbase+1);
        float upperLeftT1 = getEdgeValue(xbase, ybase+1, tbase+1);

        float upperAverageT0 = upperLeftT0 + xFraction * (upperRightT0 - upperLeftT0);
        float lowerAverageT0 = lowerLeftT0 + xFraction * (lowerRightT0 - lowerLeftT0);
        float vT0 = lowerAverageT0 + yFraction * (upperAverageT0 - lowerAverageT0);

        float upperAverageT1 = upperLeftT1 + xFraction * (upperRightT1 - upperLeftT1);
        float lowerAverageT1 = lowerLeftT1 + xFraction * (lowerRightT1 - lowerLeftT1);
        float vT1 = lowerAverageT1 + yFraction * (upperAverageT1 - lowerAverageT1);

        return vT0 + tFraction * (vT1-vT0);
    }

    // Adapted from ImageJ's source
    private float getEdgeValue(int x, int y, int t) {
        if (x<=0) x = 0;
        if (x>=width) x = width-1;
        if (y<=0) y = 0;
        if (y>=height) y = height-1;
        if (t<=0) t = 0;
        if (t>=nTimePoints) t = nTimePoints-1;
        return pixelsOriginal[getIdx(x, y, t)];
    }
}