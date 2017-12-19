package nanoj.core.java.featureExtraction;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.image.transform.NormalizedCrossCorrelationMap;
import nanoj.core.java.projections.Projections2D;

import static nanoj.core.java.array.ArrayMath.*;
import static nanoj.core.java.image.calculator.ImageCalculator.meanSquareError;
import static nanoj.core.java.image.calculator.ImageCalculator.normalize;
import static nanoj.core.java.image.drift.EstimateShiftAndTilt.getCenterOfMass;

/**
 * Created by paxcalpt on 07/06/15.
 */
public class GenerateModelParticle {
    private static final NormalizedCrossCorrelationMap NCCMap = new NormalizedCrossCorrelationMap();

    /**
     *
     * @param ims
     * @param nIterations
     * @return {ImageStack with evolution of particle model, ImageStack with MSE evolution of particle model}
     */
    public static ImageStack[] estimateAndRealignModelParticle(ImageStack ims, int nIterations) {
        FloatProcessor ipModel;
        ImageStack imsOriginal = normalize(ims);
        ims = imsOriginal.duplicate();

        ImageStack imsParticleEstimation = new ImageStack(ims.getWidth(), ims.getHeight());
        ImageStack imsParticleSE = new ImageStack(ims.getWidth(), ims.getHeight());

        ipModel = Projections2D.averageProjection(ims);
        recenterAndNormalizeModel(ipModel);

        float oldMSE = Float.MAX_VALUE, newMSE;
        for (int i=1; i<=nIterations; i++) {
            ImageStack imsCCMap = NCCMap.calculate(ipModel, imsOriginal, ipModel.getWidth(), ipModel.getHeight());

            float[] similarity = new float[imsCCMap.getSize()];
            for (int n=1; n<=imsCCMap.getSize(); n++) {
                FloatProcessor ipNCCMap = (FloatProcessor) imsCCMap.getProcessor(n);
                int w = ipNCCMap.getWidth();
                int h = ipNCCMap.getHeight();

                float[] pMax = getCenterOfMass(ipNCCMap);
                float xShift = pMax[0] - w / 2f - 0.5f;
                float yShift = pMax[1] - h / 2f - 0.5f;

                FloatProcessor ip = (FloatProcessor) imsOriginal.getProcessor(n).duplicate();
                ip.setInterpolationMethod(ImageProcessor.BICUBIC);
                ip.translate(-xShift, -yShift);
                ip.min(getMinNonZeroValue((float[]) ip.getPixels())[1]);
                normalize(ip);

                ims.setProcessor(ip, n);

//                // skip if shift is too big
//                if (abs(xShift) > 0.25 * w || abs(yShift) > 0.25 * h) {
//                    similarity[n-1] = 0;
//                    continue;
//                }
//                else {
//                    similarity[n - 1] = (float) calculatePPMCC((float[]) ipModel.getPixels(), (float[]) ip.getPixels(), true);
//                }
                similarity[n - 1] = (float) calculatePPMCC((float[]) ipModel.getPixels(), (float[]) ip.getPixels(), true);
            }

            double similaritySum = 0;
            for (int n=0; n<similarity.length; n++) similaritySum += similarity[n];
            for (int n=0; n<similarity.length; n++) similarity[n] /= similaritySum;

            ipModel = Projections2D.weightedAverageProjection(ims, similarity);
            recenterAndNormalizeModel(ipModel);

            ImageProcessor ipSE = meanSquareError(ipModel, ims);
            newMSE = getAverageValue((float[]) ipSE.getPixels());

            if (newMSE > oldMSE) {
                break;
            }
            else {
                oldMSE = newMSE;
                imsParticleEstimation.addSlice(ipModel);
                imsParticleEstimation.setSliceLabel("MSE=" + newMSE + " N=" + ims.getSize(), imsParticleEstimation.size());
                imsParticleSE.addSlice(ipSE);
                imsParticleSE.setSliceLabel("MSE=" + newMSE + " N=" + ims.getSize(), imsParticleSE.size());
            }
        }

        return new ImageStack[] {imsParticleEstimation, imsParticleSE};
    }

    private static void recenterAndNormalizeModel(FloatProcessor ip) {
        float[] pMax = getCenterOfMass(ip);
        float xShift = pMax[0] - ip.getWidth() / 2f - 0.5f;
        float yShift = pMax[1] - ip.getHeight() / 2f - 0.5f;
        ip.setInterpolationMethod(ImageProcessor.BICUBIC);
        ip.translate(-xShift, -yShift);
        ip.min(getMinNonZeroValue((float[]) ip.getPixels())[1]);
        normalize(ip);
    }
}
