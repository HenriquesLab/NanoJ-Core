package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.gui._BaseDialog_;

import static nanoj.core.java.image.transform.PolarTransform.polarTransform;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/02/2016
 * Time: 10:34
 */
public class PolarTransform_ extends _BaseDialog_ {

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Polar Map...");
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {

        showPreview = gd.getNextBoolean();

        savePrefs();
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();

        ImageStack imsPolar = null;

        for (int n=1; n<=ims.getSize(); n++) {
            FloatProcessor ipPolar = polarTransform(ims.getProcessor(n).convertToFloatProcessor());
            if (imsPolar == null) imsPolar = new ImageStack(ipPolar.getWidth(), ipPolar.getHeight());
            imsPolar.addSlice(ipPolar);
        }

        ImagePlus impResults = new ImagePlus("Polar Map", imsPolar);
        impResults.show();
    }

    public void doPreview() {
        FloatProcessor ipPolar = polarTransform(imp.getProcessor().convertToFloatProcessor());
        if (impPreview != null) impPreview.setProcessor(ipPolar);
        else impPreview = new ImagePlus("Polar Map", ipPolar);

        impPreview.show();

    }
}
