package nanoj.core.java.gui.tools.filtering.convolution;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.filtering.Convolve;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 04/01/15
 * Time: 16:29
 */
public class _BaseConvolutionDialog_ extends _BaseDialog_ {
    Convolve cv = new Convolve();
    FloatProcessor kernel;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    @Override
    public void setupDialog() {
    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void doPreview() {
        if (impPreview == null)
            impPreview = new ImagePlus("Preview...", imp.getProcessor().duplicate());
        else
            impPreview.setProcessor(imp.getProcessor().duplicate());
        cv.convolve2DStack(impPreview, kernel);
        impPreview.show();
    }

    @Override
    public void execute(){
        if (imp == null) imp = IJ.getImage();
        cv.convolve2DStack(imp, kernel);
    }
}
