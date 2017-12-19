package nanoj.core.java.gui.tools.filtering.convolution;

import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.image.filtering.ConvolutionKernels;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 20:53
 */
public class LoG_ extends _BaseConvolutionDialog_ {

    @Override
    public void setupDialog(){
        gd = new NonBlockingGenericDialog("Laplacian of Gaussian Convolution...");
        gd.addNumericField("Gaussian Sigma", prefs.get("NJ.Common.Filtering.Convolution.LoG.Sigma", 1.5f), 2);
        gd.addCheckbox("Preview", false);
    }

    @Override
    public boolean loadSettings() {
        double sigma = gd.getNextNumber();
        prefs.set("NJ.Common.Filtering.Convolution.LoG.Sigma", sigma);
        kernel = ConvolutionKernels.genLaplacianOfGaussianKernel((float) sigma);
        showPreview = gd.getNextBoolean();
        return true;
    }
}
