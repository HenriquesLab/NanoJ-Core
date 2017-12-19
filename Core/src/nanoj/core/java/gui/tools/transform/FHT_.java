package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.transform.NJ_FHT;

import static nanoj.core.java.image.transform.NJ_FHT.makeEvenSquare;

/**
 * Created by Henriques-lab on 27/09/2016.
 */
public class FHT_ extends _BaseDialog_ {
    String title;
    boolean inverse = false;
    boolean swapQuadrants = true;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Discrete Hartley Transform...");
        gd.addCheckbox("Swap quadrants", getPrefs("swapQuadrants", true));
        gd.addCheckbox("Inverse", getPrefs("inverse", false));
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        swapQuadrants = gd.getNextBoolean();
        inverse = gd.getNextBoolean();

        showPreview = gd.getNextBoolean();

        title = imp.getTitle();
        if (inverse) title +=" - inverse FHT";
        else title += " - FHT";

        setPrefs("swapQuadrants", swapQuadrants);
        setPrefs("inverse", inverse);
        savePrefs();
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();

        ImageStack imsOut = null;

        for (int s=1; s<=ims.getSize(); s++) {
            FloatProcessor fp = ims.getProcessor(s).convertToFloatProcessor();
            fp = processFrame(fp);
            if (imsOut == null) imsOut = new ImageStack(fp.getWidth(), fp.getHeight());
            imsOut.addSlice(fp);
        }

        ImagePlus impResults = new ImagePlus(title, imsOut);
        impResults.show();
    }

    public void doPreview() {
        ImageProcessor ip = imp.getProcessor().convertToFloatProcessor();
        FloatProcessor fp = ip.convertToFloatProcessor();
        fp = processFrame(fp);
        if (impPreview != null) impPreview.setProcessor(fp);
        else if (inverse) impPreview = new ImagePlus("Preview - inverse FHT", fp);
        else impPreview = new ImagePlus("Preview - FHT", fp);
        impPreview.show();

    }

    public FloatProcessor processFrame(FloatProcessor fp) {
        fp = (FloatProcessor) makeEvenSquare(fp);

        if (!inverse) {
            fp = NJ_FHT.forwardFHT(fp);
            if (swapQuadrants) NJ_FHT.swapQuadrants(fp);
        }
        else {
            if (swapQuadrants) NJ_FHT.swapQuadrants(fp);
            fp = NJ_FHT.inverseFHT(fp, true);
        }
        return fp;
    }
}
