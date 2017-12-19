package nanoj.core.java.image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ImageStackToFromArray;
import nanoj.core.java.tools.Log;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/02/15
 * Time: 15:31
 */
public class NormalizedCrossCorrelationMap {

    private static Kernel_NormalizedCrossCorrelationMap CCMKernel = new Kernel_NormalizedCrossCorrelationMap();
    private Log log = new Log();

    public String getExecutionMode(){
        return CCMKernel.getExecutionMode().toString();
    }

    public FloatProcessor calculate(FloatProcessor referenceFrame, FloatProcessor comparisonFrame,
                                    int radiusX, int radiusY){
        int width  = referenceFrame.getWidth();
        int height = referenceFrame.getHeight();

        assert (width ==comparisonFrame.getWidth());
        assert (height==comparisonFrame.getHeight());

        float[] pixelsReference  = (float[]) referenceFrame.getPixels();
        float[] pixelsComparison = (float[]) comparisonFrame.getPixels();

        float[] CCMap = calculate(pixelsReference, pixelsComparison, width, height, radiusX, radiusY);

        return new FloatProcessor(CCMKernel.CCMapWidth, CCMKernel.CCMapHeight, CCMap);
    }

    public ImageStack calculate(ImageStack ims, int radiusX, int radiusY){

        int width  = ims.getWidth();
        int height = ims.getHeight();
        radiusX = Math.min(radiusX, width /2);
        radiusY = Math.min(radiusY, height/2);

        log.msg(4, "NormalizedCrossCorrelationMap: doing cross-correlation");
        float[] pixelsStack = ImageStackToFromArray.ImageStackToFloatArray(ims);
        float[] CCMap = CCMKernel.calculate(pixelsStack, width, height, radiusX, radiusY);

        return ImageStackToFromArray.ImageStackFromFloatArray(CCMap, CCMKernel.CCMapWidth, CCMKernel.CCMapHeight);
    }

    public ImageStack calculate(FloatProcessor referenceFrame, ImageStack ims,
                                int radiusX, int radiusY){

        int width  = referenceFrame.getWidth();
        int height = referenceFrame.getHeight();

        assert (width ==ims.getWidth());
        assert (height==ims.getHeight());

        float[] pixelsReference = (float[]) referenceFrame.getPixels();
        float[] pixelsStack = ImageStackToFromArray.ImageStackToFloatArray(ims);

        float[] CCMap = calculate(pixelsReference, pixelsStack, width, height, radiusX, radiusY);

        return ImageStackToFromArray.ImageStackFromFloatArray(CCMap, CCMKernel.CCMapWidth, CCMKernel.CCMapHeight);
    }

    public float[] calculate(float[] pixelsReference, float[] pixelsStack, int width, int height,
                             int radiusX, int radiusY){

        radiusX = Math.min(radiusX, width /2);
        radiusY = Math.min(radiusY, height/2);

        log.msg(4, "NormalizedCrossCorrelationMap: doing cross-correlation");
        CCMKernel.setupReferenceFrame(pixelsReference, width, height);

        return CCMKernel.calculate(pixelsStack, width, height, radiusX, radiusY);
    }
}

class Kernel_NormalizedCrossCorrelationMap extends NJKernel {

    private float[] CCMap, pixelsReference, pixelsStack, pixelsMeanForFrame;
    private int CCMapWidthHeight, width, height, widthHeight, radiusX, radiusY, nTimePoints;
    public int CCMapWidth, CCMapHeight;
    public int stepFlag = 0;
    public int hasReference = 0;

    Log log = new Log();

    public void setupReferenceFrame(float[] pixelsReference, int width, int height){
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.pixelsReference = pixelsReference.clone();

        float mean = 0;
        int counter = 0;
        // Calculate mean
        for (int n=0;n<widthHeight;n++) {
            if (this.pixelsReference[n] != 0) {
                mean += this.pixelsReference[n];
                counter++;
            }
        }

        mean /= counter;
        // Calculate delta
        for (int n=0;n<widthHeight;n++) {
            if (this.pixelsReference[n] != 0)
                this.pixelsReference[n] -= mean;
        }
        hasReference = 1;
    }

    public float[] calculate(float[] pixelsStack, int width, int height, int radiusX, int radiusY) {
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;

        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.nTimePoints = pixelsStack.length/widthHeight;
        this.CCMapWidth  = radiusX*2+1;
        this.CCMapHeight = radiusY*2+1;
        this.CCMapWidthHeight = this.CCMapWidth*this.CCMapHeight;
        this.CCMap = new float[CCMapWidthHeight * nTimePoints];

        this.pixelsMeanForFrame = new float[this.nTimePoints];
        this.pixelsStack = pixelsStack;
        if (this.hasReference == 0) this.pixelsReference = new float[] {0};

        // Upload arrays
        setExplicit(true);
        if (width > 100 && height > 100) setExecutionMode(EXECUTION_MODE.JTP);
        else autoChooseDeviceForNanoJ();

        put(this.pixelsReference);
        put(this.pixelsStack);
        put(this.pixelsMeanForFrame);
        put(this.CCMap);

        log.msg(3, "Kernel_CrossCorrelationMap: calculating frame-mean and pixel-delta");
        stepFlag = 1;
        execute(pixelsMeanForFrame.length);

        log.msg(3, "Kernel_CrossCorrelationMap: calculating cross-correlation");
        stepFlag = 2;
        execute(CCMapWidthHeight * nTimePoints);

        // Download arrays
        get(this.CCMap);
        return this.CCMap;
    }

    // called inside CL
    @Override public void run() {
        if (stepFlag == 1)
            calculateMeanAndDeltaFrames();
        else if (stepFlag == 2)
            calculateCrossCorrelation();
    }

    // called inside CL
    public void calculateMeanAndDeltaFrames() {
        int t = getGlobalId();
        int idx = 0;
        int counter = 0;
        // calculate mean
        for (int n=0;n<widthHeight;n++) {
            idx = t * widthHeight + n;
            if (pixelsStack[idx] != 0) {
                pixelsMeanForFrame[t] += pixelsStack[idx];
                counter++;
            }
        }
        pixelsMeanForFrame[t] /= counter;
        // calculate delta
        for (int n=0;n<widthHeight;n++) {
            idx = t * widthHeight + n;
            if (pixelsStack[idx] != 0)
                pixelsStack[idx] -= pixelsMeanForFrame[t];
        }
    }

    // called inside CL
    private void calculateCrossCorrelation() {
        int pixelIdx = getGlobalId();

        int xCC = pixelIdx % CCMapWidth;
        int yCC = (pixelIdx / CCMapWidth) % CCMapHeight;
        int t = pixelIdx / CCMapWidthHeight;
        int shiftX = xCC-radiusX;
        int shiftY = yCC-radiusY;

        float v0 = 0, v1 = 0;
        int x0, y0, x1, y1;

        float covariance = 0;
        float squareSum1  = 0;
        float squareSum2  = 0;

        int xStart = 0;
        int yStart = 0;
        int xEnd   = width-1;
        int yEnd   = height-1;
        if (shiftX < 0) xStart = -shiftX;
        else xEnd = xEnd - shiftX;
        if (shiftY < 0) yStart = -shiftY;
        else yEnd = yEnd - shiftY;

        for (y0=yStart;y0<=yEnd;y0++) {
            y1 = y0 + shiftY;
            for (x0=xStart;x0<=xEnd;x0++) {
                x1 = x0 + shiftX;

                if (this.hasReference == 1) v0 = pixelsReference[y0*width+x0];
                else v0 = pixelsStack[getIdx(x0, y0, max(t-1, 0))];
                v1 = pixelsStack[getIdx(x1, y1, t)];
                covariance += v0*v1;
                squareSum1 += v0*v0;
                squareSum2 += v1*v1;
            }
        }
        if (squareSum1 > 0.01f && squareSum2 > 0.01f)
            CCMap[pixelIdx] = covariance / sqrt(squareSum1 * squareSum2);
    }

    private int getIdx(int x, int y, int t){
        return t * widthHeight + y * width + x;
    }
}