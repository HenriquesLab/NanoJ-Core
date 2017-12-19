package nanoj.core.java.image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.image.calculator.ImageCalculator;
import nanoj.core.java.tools.Log;

import static java.lang.Math.PI;
import static nanoj.core.java.array.ArrayInitialization.initializeFloatAndGrowthFill;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 30/04/15
 * Time: 13:55
 */
public class NormalizedRotationAndCrossCorrelationMap {

    private static final TranslateOrRotateImage translateOrRotateImage = new TranslateOrRotateImage();
    private static final NormalizedCrossCorrelationMap normalizedCrossCorrelationMap = new NormalizedCrossCorrelationMap();
    private static final Log log = new Log();

    public boolean showProgress = false;

    public ImageStack calculate(FloatProcessor referenceFrame, FloatProcessor comparisonFrame,
                                float angleStep) {
        ImageStack ims = new ImageStack(comparisonFrame.getWidth(), comparisonFrame.getHeight());
        ims.addSlice(comparisonFrame);
        return calculate(referenceFrame, ims, angleStep)[0];
    }

    public ImageStack calculate(FloatProcessor referenceFrame, FloatProcessor comparisonFrame,
                                float angleStep, int radiusX, int radiusY, boolean squareData) {
        ImageStack ims = new ImageStack(comparisonFrame.getWidth(), comparisonFrame.getHeight());
        ims.addSlice(comparisonFrame);
        return calculate(referenceFrame, ims, angleStep, squareData)[0];
    }

    public ImageStack[] calculate(FloatProcessor referenceFrame, ImageStack ims,
                                  float angleStep, boolean squareData) {
        if (squareData) {
            referenceFrame = (FloatProcessor) referenceFrame.duplicate(); referenceFrame.sqr();
            ims = ImageCalculator.square(ims);
        }
        return calculate(referenceFrame, ims, angleStep);
    }

    public ImageStack[] calculate(FloatProcessor referenceFrame, ImageStack ims,
                                  float angleStep) {

        // Prepare to rotate frames
        int nSlices = ims.getSize();
        int nAngles = (int) (2.*PI / angleStep);
        float[] angles = initializeFloatAndGrowthFill(nAngles, 0, angleStep);
        ImageStack[] imsRCCMap = new ImageStack[ims.getSize()];

        // Rotate and then calculate Cross Correlation
        for (int s=1; s<=nSlices; s++) {

            if (showProgress) log.progress(s, nSlices);

            // build rotated stack
            ImageStack imsRotated = new ImageStack(referenceFrame.getWidth(), referenceFrame.getHeight());
            for (int n=0; n<angles.length; n++) {
                imsRotated.addSlice(ims.getProcessor(s));
            }

            // rotate
            imsRotated = translateOrRotateImage.rotate(imsRotated, angles);

            int radius = (int) (ims.getWidth()*0.5);
            imsRCCMap[s-1] = normalizedCrossCorrelationMap.calculate(referenceFrame, imsRotated, radius, radius);
            //imsRCCMap[s-1] = calculateCrossCorrelationMap(referenceFrame, imsRotated, true);
        }

        return imsRCCMap;
    }

    public ImageStack[] calculateDirect(FloatProcessor referenceFrame, ImageStack ims,
                                  float angleStep, int radiusX, int radiusY) {

        // Prepare to rotate frames
        int nSlices = ims.getSize();
        int nAngles = (int) (2.*PI / angleStep);
        float[] angles = initializeFloatAndGrowthFill(nAngles, 0, angleStep);
        ImageStack[] imsRCCMap = new ImageStack[ims.getSize()];

        // Rotate and then calculate Cross Correlation
        for (int s=1; s<=nSlices; s++) {

            if (showProgress) log.progress(s, nSlices);

            // build rotated stack
            ImageStack imsRotated = new ImageStack(referenceFrame.getWidth(), referenceFrame.getHeight());
            for (int n=0; n<angles.length; n++) {
                imsRotated.addSlice(ims.getProcessor(s));
            }

            // rotate
            imsRotated = translateOrRotateImage.rotate(imsRotated, angles);

            imsRCCMap[s-1] = normalizedCrossCorrelationMap.calculate(referenceFrame, imsRotated, radiusX, radiusY);
            //imsRCCMap[s-1] = calculateCrossCorrelationMap(referenceFrame, imsRotated, true);
        }

        return imsRCCMap;
    }

    //Joint NRCCM
    public ImageStack[] calculate(FloatProcessor[] referenceFrames, ImageStack[] imss,
                                  float angleStep) {
        int nChannels = referenceFrames.length;
        //set JNRCCM to first channel
        ImageStack[] imsJRCCMap = calculate(referenceFrames[0], imss[0], angleStep);
        //add additional channels
        for (int c=1;c<nChannels;c++) {
            ImageStack[] imsRCCMapC = calculate(referenceFrames[c], imss[c], angleStep);
            for (int i = 0; i < imsJRCCMap.length; i++) {
                for (int p = 1; p <= imsRCCMapC[0].getSize(); p++) {
                    ImageProcessor ip = ImageCalculator.add(imsJRCCMap[i].getProcessor(p), imsRCCMapC[i].getProcessor(p));
                    imsJRCCMap[i].setProcessor(ip,p);
                }
            }
        }
        //normalise
        for(int i=0;i<imsJRCCMap.length;i++){
            for(int p=1;p<= imsJRCCMap[0].getSize();p++){
                imsJRCCMap[i].getProcessor(p).multiply(1d/nChannels);
            }
        }
        return imsJRCCMap;
    }

}


























