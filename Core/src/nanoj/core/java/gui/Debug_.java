package nanoj.core.java.gui;

import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/01/15
 * Time: 13:47
 */
public class Debug_ implements PlugIn {

    Prefs prefs = new Prefs();

    @Override
    public void run(String s) {

        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Show debug information...");
        gd.addSlider("Debug level (0 silences it)", 0, 8, prefs.get("NJ.debugLevel", 2));
        gd.addCheckbox("Use debug choices on dialogs", prefs.get("NJ.debugChoices", false));
        gd.addCheckbox("Use OpenCL Safe Mode (Java Thread Pool)",prefs.get("NJ.kernelMode",false));

        // Show dialog
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        prefs.set("NJ.debugLevel", gd.getNextNumber());
        prefs.set("NJ.debugChoices", gd.getNextBoolean());
        prefs.set("NJ.kernelMode", gd.getNextBoolean());
        prefs.savePreferences();
    }
}

