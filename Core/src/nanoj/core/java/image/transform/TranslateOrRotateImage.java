package nanoj.core.java.image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ArrayCasting;
import nanoj.core.java.array.ArrayInitialization;
import nanoj.core.java.tools.Options;

import static nanoj.core.java.array.ImageStackToFromArray.*;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class TranslateOrRotateImage {

    public final static Kernel_TranslateOrRotateImage kernel = new Kernel_TranslateOrRotateImage();
    public final int NEAREST_NEIGHBOUR = Options.NEAREST_NEIGHBOUR;
    public final int BILINEAR = Options.BILINEAR;
    public final int BICUBIC = Options.BICUBIC;

    public void setInterpolationMethod(int method) {
        kernel.interpolationMethod = method;
    }

    public ImageStack translate(ImageStack ims, float shiftX, float shiftY){
        float[] shiftX_ = ArrayInitialization.initializeAndValueFill(ims.getSize(), shiftX);
        float[] shiftY_ = ArrayInitialization.initializeAndValueFill(ims.getSize(), shiftY);
        return translate(ims, shiftX_, shiftY_);
    }

    public ImageStack translate(ImageStack ims, float[] shiftX, float[] shiftY){
        assert (ims.getSize() == shiftX.length && shiftX.length == shiftY.length);
        int width = ims.getWidth();
        int height = ims.getHeight();
        float[] pixelsOriginal = ImageStackToFloatArray(ims);
        float[] pixelsTranslated = translate(pixelsOriginal, width, height, shiftX, shiftY);
        if (ims.getBitDepth() == 16)
            return ImageStackFromShortArray(ArrayCasting.floatToShort(pixelsTranslated), width, height );
        return ImageStackFromFloatArray(pixelsTranslated, width, height);
    }

    public FloatProcessor translate(FloatProcessor fp, float shiftX, float shiftY){
        float[] pixelsTranslated = translate((float[]) fp.getPixels(), fp.getWidth(), fp.getHeight(), shiftX, shiftY);
        return new FloatProcessor(fp.getWidth(), fp.getHeight(), pixelsTranslated);
    }

    public float[] translate(float[] pixels, int width, int height, float shiftX, float shiftY){
        if (shiftX-(int)shiftX==0 && shiftY-(int)shiftY==0) kernel.isShiftInt = 1;
        int nTimePoints = pixels.length/(width*height);
        float[] shiftX_ = ArrayInitialization.initializeAndValueFill(nTimePoints, shiftX);
        float[] shiftY_ = ArrayInitialization.initializeAndValueFill(nTimePoints, shiftY);
        pixels = translate(pixels, width, height, shiftX_, shiftY_);
        kernel.isShiftInt = 0;
        return pixels;
    }

    public float[] translate(float[] pixels, int width, int height, float[] shiftX, float[] shiftY){
        return kernel.translate(pixels, width, height, shiftX, shiftY);
    }

    public ImageStack rotate(ImageStack ims, float angle) {
        float[] rotationAngle = ArrayInitialization.initializeAndValueFill(ims.getSize(), angle);
        return rotate(ims, rotationAngle);
    }

    public ImageStack rotate(ImageStack ims, float[] angle){
        assert (ims.getSize() == angle.length);
        int width = ims.getWidth();
        int height = ims.getHeight();
        float[] pixelsOriginal = ImageStackToFloatArray(ims);
        float[] pixelsRotated = rotate(pixelsOriginal, width, height, angle);
        if (ims.getBitDepth() == 16)
            return ImageStackFromShortArray(ArrayCasting.floatToShort(pixelsRotated), width, height );
        return ImageStackFromFloatArray(pixelsRotated, width, height);
    }

    public FloatProcessor rotate(FloatProcessor fp, float angle){
        float[] pixelsTranslated = rotate((float[]) fp.getPixels(), fp.getWidth(), fp.getHeight(), angle);
        return new FloatProcessor(fp.getWidth(), fp.getHeight(), pixelsTranslated);
    }

    public float[] rotate(float[] pixels, int width, int height, float angle){
        int nTimePoints = pixels.length/(width*height);
        float[] angle_ = ArrayInitialization.initializeAndValueFill(nTimePoints, angle);
        pixels = rotate(pixels, width, height, angle_);
        return pixels;
    }

    public float[] rotate(float[] pixels, int width, int height, float[] angle){
        return kernel.rotate(pixels, width, height, angle);
    }

    public ImageStack translateAndRotate(ImageStack ims, float[] shiftX, float[] shiftY, float[] angle) {
        ImageStack imsOriginal = ims;
        ims = translate(ims, shiftX, shiftY);
        ims = rotate(ims, angle);
        for (int n=1; n<ims.getSize(); n++) ims.setSliceLabel(imsOriginal.getSliceLabel(n), n);
        return ims;
    }

    public ImageStack translateAndRotate(ImageStack ims, float[][] shiftAndTilt) {
        float[] shiftX = shiftAndTilt[1];
        float[] shiftY = shiftAndTilt[2];
        float[] theta  = shiftAndTilt[3];
        return translateAndRotate(ims, shiftX, shiftY, theta);
    }

    public FloatProcessor translateAndRotate(FloatProcessor fp, float shiftX, float shiftY, float angle) {
        fp = translate(fp, shiftX, shiftY);
        fp = rotate(fp, angle);
        return fp;
    }
}

class Kernel_TranslateOrRotateImage extends NJKernel {

    //private static float PI = (float) Math.PI;

    public final int NEAREST_NEIGHBOUR = Options.NEAREST_NEIGHBOUR;
    public final int BILINEAR = Options.BILINEAR;
    public final int BICUBIC = Options.BICUBIC;

    private float[] pixelsOriginal, pixelsTranslatedOrRotated, shiftX, shiftY, angle;
    private int width, height, widthHeight;
    public int interpolationMethod = BICUBIC;
    public int isShiftInt = 0;
    public int doShift = 1;

    public float[] translate(float[] pixels, int width, int height, float[] shiftX, float[] shiftY) {
        this.doShift = 1;
        this.pixelsOriginal = pixels;
        this.pixelsTranslatedOrRotated = new float[pixels.length];
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
        this.angle = new float[0];

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixelsOriginal);
        put(this.pixelsTranslatedOrRotated);
        put(this.shiftX);
        put(this.shiftY);
        put(this.angle);

        execute(pixelsOriginal.length);

        // Download arrays
        get(this.pixelsTranslatedOrRotated);
        return this.pixelsTranslatedOrRotated;
    }

    public float[] rotate(float[] pixels, int width, int height, float[] angle) {
        this.doShift = 0;
        this.pixelsOriginal = pixels;
        this.pixelsTranslatedOrRotated = new float[pixels.length];
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.shiftX = new float[0];
        this.shiftY = new float[0];
        this.angle = angle;

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixelsOriginal);
        put(this.pixelsTranslatedOrRotated);
        put(this.shiftX);
        put(this.shiftY);
        put(this.angle);

        execute(pixelsOriginal.length);

        // Download arrays
        get(this.pixelsTranslatedOrRotated);
        return this.pixelsTranslatedOrRotated;
    }

    @Override public void run() {
        if (doShift == 1) translate();
        else rotate();
    }

    public void translate() {

        int p = getGlobalId();
        int x = p % (width);
        int y = (p / width) % height;
        int t = p / (widthHeight);

        float xShifted_float = (x - shiftX[t]);
        float yShifted_float = (y - shiftY[t]);
        int xShifted_int = (int)(xShifted_float);
        int yShifted_int = (int)(yShifted_float);

        if (xShifted_int<0 || yShifted_int<0 || xShifted_int>=width || yShifted_int>=height) return;

        if (isShiftInt == 1) {
            pixelsTranslatedOrRotated[p] = pixelsOriginal[getIdx(xShifted_int, yShifted_int, t)];
            return;
        }
        if (interpolationMethod == NEAREST_NEIGHBOUR)
            pixelsTranslatedOrRotated[p] = getClosest(xShifted_float, yShifted_float, t);
        else if (interpolationMethod == BILINEAR)
            pixelsTranslatedOrRotated[p] = getBilinearInterpolatedPixel(xShifted_float, yShifted_float, t);
        else
            pixelsTranslatedOrRotated[p] = getBicubicInterpolatedPixel(xShifted_float, yShifted_float, t);
    }

    public void rotate() {
        int p = getGlobalId();
        int x = p % (width);
        int y = (p / width) % height;
        int t = p / (widthHeight);

        float centerX = (width-1f)/2f;
        float centerY = (height-1f)/2f;
        //float angleRadians = angle[t]/(180.0f/PI);
        float angleRadians = angle[t];

        float ca = cos(angleRadians);
        float sa = sin(angleRadians);

        float xs=(x-centerX)*ca-(y-centerY)*sa+centerX;
        float ys=(x-centerX)*sa+(y-centerY)*ca+centerY;

        if (interpolationMethod == NEAREST_NEIGHBOUR)
            pixelsTranslatedOrRotated[p] = getClosest(xs, ys, t);
        else if (interpolationMethod == BILINEAR)
            pixelsTranslatedOrRotated[p] = getBilinearInterpolatedPixel(xs, ys, t);
        else
            pixelsTranslatedOrRotated[p] = getBicubicInterpolatedPixel(xs, ys, t);
    }


    private int getIdx(int x, int y, int t){
        return t * widthHeight + y * width + x;
    }

    private float getClosest(float x, float y, int t) {
        int x_, y_;
        x_ = round(x);
        y_ = round(y);
        x_ = min(max(x_, 0), width - 1);
        y_ = min(max(y_, 0), height - 1);
        return pixelsOriginal[getIdx(x_, y_, t)];
    }

    // Adapted from ImageJ's source
    private float getInterpolatedPixel(float x, float y, int t){
        if (x<0.0f || x>=width-1.0f || y<0.0f || y>=height-1.0f) {
            if (x<-1.0f || x>=width || y<-1.0f || y>=height)
                return 0;
            else
                return getInterpolatedEdgeValue(x, y, t);
        }
        int xbase = (int)x;
        int ybase = (int)y;
        float xFraction = x - xbase;
        float yFraction = y - ybase;

        if (xFraction<0) xFraction = 0;
        if (yFraction<0) yFraction = 0;

        float lowerLeft = pixelsOriginal[getIdx(xbase, ybase, t)];
        float lowerRight = pixelsOriginal[getIdx(xbase+1, ybase, t)];
        float upperRight = pixelsOriginal[getIdx(xbase+1, ybase+1, t)];
        float upperLeft = pixelsOriginal[getIdx(xbase, ybase+1, t)];
        float upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
        float lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);

        return lowerAverage + yFraction * (upperAverage - lowerAverage);
    }

    // Adapted from ImageJ's source
    private float getBilinearInterpolatedPixel(float x, float y, int t) {
        float value;
        if (x>=-1 && x<width && y>=-1 && y<height) {
            value = getInterpolatedPixel(x, y, t);
            return value;
        } else
            return 0;
    }

    // Adapted from ImageJ's source
    public float getBicubicInterpolatedPixel(float x0, float y0, int t) {
        int u0 = (int) floor(x0);  //use floor to handle negative coordinates too
        int v0 = (int) floor(y0);
        if (u0<=0 || u0>=width-2 || v0<=0 || v0>=height-2)
            return getBilinearInterpolatedPixel(x0, y0, t);
        float q = 0;
        float p = 0;
        int u = 0;
        int v = 0;
        for (int j = 0; j <= 3; j++) {
            v = v0 - 1 + j;
            p = 0;
            for (int i = 0; i <= 3; i++) {
                u = u0 - 1 + i;
                p = p + pixelsOriginal[getIdx(u,v,t)] * cubic(x0 - u);
            }
            q = q + p * cubic(y0 - v);
        }
        return q;
    }

    // Adapted from ImageJ's source
    private float cubic(float x) {
        float a = 0.5f; // Catmull-Rom interpolation
        if (x < 0.0f) x = -x;
        float z = 0.0f;
        if (x < 1.0f)
            z = x * x * (x * (-a + 2.0f) + (a - 3.0f)) + 1.0f;
        else if (x < 2.0f)
            z = -a * x * x * x + 5.0f * a * x * x - 8.0f * a * x + 4.0f * a;
        return z;
    }

    // Adapted from ImageJ's source
    private float getInterpolatedEdgeValue(float x, float y, int t) {
        int xbase = (int) x;
        int ybase = (int) y;
        float xFraction = x - xbase;
        float yFraction = y - ybase;
        if (xFraction<0) xFraction = 0;
        if (yFraction<0) yFraction = 0;
        float lowerLeft = getEdgeValue(xbase, ybase, t);
        float lowerRight = getEdgeValue(xbase+1, ybase, t);
        float upperRight = getEdgeValue(xbase+1, ybase+1, t);
        float upperLeft = getEdgeValue(xbase, ybase+1, t);
        float upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
        float lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);
        return lowerAverage + yFraction * (upperAverage - lowerAverage);
    }

    // Adapted from ImageJ's source
    private float getEdgeValue(int x, int y, int t) {
        if (x<=0) x = 0;
        if (x>=width) x = width-1;
        if (y<=0) y = 0;
        if (y>=height) y = height-1;
        return pixelsOriginal[getIdx(x, y, t)];
    }

}