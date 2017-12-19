package nanoj.core.java.gui.tools.filtering.convolution3D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import nanoj.core.java.image.filtering.Convolve;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 04/01/15
 * Time: 16:29
 */
public class _BaseDialog_ implements PlugIn {
    NonBlockingGenericDialog gd;
    Prefs prefs = new Prefs();
    ImagePlus imp = null;
    ImagePlus impPreview = null;
    boolean _preview = false;

    Convolve cv = new Convolve();
    ImageStack kernel;

    @Override
    public void run(String arg) {

        setupDialog();

        // Add listener to dialog
        MyDialogListener dl = new MyDialogListener();
        gd.addDialogListener(dl);

        // Show dialog
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        afterDoPreview();
        execute();
        prefs.savePreferences();
    }

    void setupDialog(){}

    void loadSettings(){}

    void doPreview() {
        if (_preview){
            if (imp == null)
                imp = IJ.getImage();
            if (impPreview == null)
                impPreview = new ImagePlus("Preview...", imp.getStack());
            else
                impPreview.setStack(imp.getStack());
            cv.convolve3DStack(impPreview, kernel);
            impPreview.show();
        }
        else if (impPreview != null)
            impPreview.hide();
    }

    void afterDoPreview(){
        if (impPreview != null)
            impPreview.hide();
    }

    void execute(){
        if (imp == null) imp = IJ.getImage();
        cv.convolve3DStack(imp, kernel);
    }

    class MyDialogListener implements DialogListener {
        @Override
        public boolean dialogItemChanged(GenericDialog gd, AWTEvent awtEvent) {
            loadSettings();
            doPreview();
            return true;
        }
    }
}
