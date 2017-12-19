package nanoj.core.java.gui.tools.filtering.convolution;

import ij.gui.NonBlockingGenericDialog;

import static nanoj.core.java.image.filtering.ConvolutionKernels.genExtendedLaplacian;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 20:53
 */
public class ExtendedLaplacian_ extends _BaseConvolutionDialog_ {

    @Override
    public void setupDialog(){
        gd = new NonBlockingGenericDialog("Extended Laplacian Convolution...");
        gd.addCheckbox("Preview", false);
    }

    @Override
    public boolean loadSettings() {
        kernel = genExtendedLaplacian();
        showPreview = gd.getNextBoolean();
        return true;
    }
}
