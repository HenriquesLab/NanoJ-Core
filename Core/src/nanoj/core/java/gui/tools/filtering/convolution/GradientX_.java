package nanoj.core.java.gui.tools.filtering.convolution;

import ij.gui.NonBlockingGenericDialog;

import static nanoj.core.java.image.filtering.ConvolutionKernels.genGradientX;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 20:53
 */
public class GradientX_ extends _BaseConvolutionDialog_ {

    @Override
    public void setupDialog(){
        gd = new NonBlockingGenericDialog("X-Gradient Convolution...");
        gd.addNumericField("X-radius", prefs.get("NJ.Common.Filtering.Convolution.Gx.xSize", 2), 0);
        gd.addNumericField("Y-radius", prefs.get("NJ.Common.Filtering.Convolution.Gx.ySize", 1), 0);
        gd.addCheckbox("Preview", false);
    }

    @Override
    public boolean loadSettings() {
        int xSize = (int) gd.getNextNumber();
        int ySize = (int) gd.getNextNumber();
        prefs.set("NJ.Common.Filtering.Convolution.Gx.xSize", xSize);
        prefs.set("NJ.Common.Filtering.Convolution.Gx.ySize", ySize);
        kernel = genGradientX(xSize, ySize);
        showPreview = gd.getNextBoolean();
        return true;
    }
}
