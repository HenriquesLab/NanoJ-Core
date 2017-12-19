package nanoj.core.java.gui.tools.filtering.convolution3D;

import ij.gui.NonBlockingGenericDialog;

import static nanoj.core.java.image.filtering.ConvolutionKernels.gen3DSmooth;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 20:53
 */
public class Smooth_ extends _BaseDialog_ {

    void setupDialog(){
        gd = new NonBlockingGenericDialog("Create 3D smooth...");
        gd.addCheckbox("Preview convolved image", false);
    }

    void loadSettings() {
        _preview = gd.getNextBoolean();
        kernel = gen3DSmooth();
    }
}
