package nanoj.core.java.gui.tools.filtering.convolution;

import ij.gui.NonBlockingGenericDialog;

import static nanoj.core.java.image.filtering.ConvolutionKernels.genDifferenceOfGaussiansKernel;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 21:27
 */
public class DoG_ extends _BaseConvolutionDialog_ {

    @Override
    public void setupDialog(){
        gd = new NonBlockingGenericDialog("Difference of Gaussians Convolution...");
        gd.addNumericField("Small Gaussian Sigma", prefs.get("NJ.Common.Filtering.Convolution.DoG.smallSigma", 1.5f), 2);
        gd.addNumericField("Large Gaussian Sigma", prefs.get("NJ.Common.Filtering.Convolution.DoG.largeSigma", 10f), 2);
        gd.addCheckbox("Preview", false);
    }

    @Override
    public boolean loadSettings() {
        double smallSigma = gd.getNextNumber();
        double largeSigma = gd.getNextNumber();
        prefs.set("NJ.Common.Filtering.Convolution.DoG.smallSigma", smallSigma);
        prefs.set("NJ.Common.Filtering.Convolution.DoG.largeSigma", largeSigma);
        kernel = genDifferenceOfGaussiansKernel((float) smallSigma, (float) largeSigma);
        showPreview = gd.getNextBoolean();
        return true;
    }
}
