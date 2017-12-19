package nanoj.core.java.image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static nanoj.core.java.array.ArrayInitialization.initializeFloatAndGrowthFill;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 30/04/15
 * Time: 13:55
 */
public class NormalizedRotationCorrelationMap {

    private static final TranslateOrRotateImage translateOrRotateImage = new TranslateOrRotateImage();

    public FloatProcessor calculate(FloatProcessor referenceFrame, FloatProcessor comparisonFrame,
                                float startAngle, float endAngle, float angleStep) {
        ImageStack ims = new ImageStack(comparisonFrame.getWidth(), comparisonFrame.getHeight());
        ims.addSlice(comparisonFrame);
        ims = calculate(referenceFrame, ims, startAngle, endAngle, angleStep);
        return ims.getProcessor(1).convertToFloatProcessor();
    }

    public ImageStack calculate(FloatProcessor referenceFrame, ImageStack ims,
                                float startAngle, float endAngle, float angleStep) {

        referenceFrame = referenceFrame.convertToFloatProcessor(); // converting duplicated internally
        ims = ims.convertToFloat();

        // Mean subtract data
        nanoj.core.java.image.calculator.ImageCalculator.meanSubtract(referenceFrame);
        for (int s=1; s<=ims.getSize(); s++) {
            FloatProcessor fp = ims.getProcessor(s).convertToFloatProcessor();
            nanoj.core.java.image.calculator.ImageCalculator.meanSubtract(fp);
            ims.setProcessor(fp, s);
        }

        // Prepare to rotate frames
        int nAngles = (int) ((endAngle-startAngle) / angleStep);
        float[] angles = initializeFloatAndGrowthFill(nAngles, startAngle, angleStep);
        ImageStack imsRCMap = new ImageStack(nAngles, 1);

        for (int s=1; s<=ims.getSize(); s++) {

            // build rotated stack
            ImageStack imsRotated = new ImageStack(referenceFrame.getWidth(), referenceFrame.getHeight());
            for (int n=0; n<angles.length; n++) {
                imsRotated.addSlice(ims.getProcessor(s));
            }

            // rotate
            imsRotated = translateOrRotateImage.rotate(imsRotated, angles);

            // calculate Rotation-Correlation Map
            float[] RCMap = new float[angles.length];

            for (int n=0; n<nAngles; n++) {
                double covariance = 0;
                double squareSum1 = 0;
                double squareSum2 = 0;

                float[] pixelsReference = (float[]) referenceFrame.getPixels();
                float[] pixelsComparison = (float[]) imsRotated.getProcessor(n+1).getPixels();

                for (int p=0; p<pixelsReference.length; p++) {
                    double v1 = pixelsReference[p];
                    double v2 = pixelsComparison[p];

                    if (abs(v1) < 0.01f || abs(v2) < 0.01f) continue;
                    covariance += v1 * v2;
                    squareSum1 += v1 * v1;
                    squareSum2 += v2 * v2;
                }

                if (squareSum1 > 0.01f && squareSum2 > 0.01f)
                    RCMap[n] = (float) (covariance / sqrt(squareSum1 * squareSum2));
            }

            imsRCMap.addSlice(new FloatProcessor(nAngles, 1, RCMap));
        }

        return imsRCMap;
    }
}
