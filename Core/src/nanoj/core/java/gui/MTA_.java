package nanoj.core.java.gui;

import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import nanoj.updater.java.MTA;

/**
 * Created by Nils Gustafsson on 17/03/15.
 */
public class MTA_ implements PlugIn{

    public Prefs prefs = new Prefs();

    public void run(String arg){
        NonBlockingGenericDialog gd_mta = new NonBlockingGenericDialog("Material Transfer Agreement");
        gd_mta.addMessage(MTA.MTA_TEXT);
        gd_mta.addCheckbox("I agree", false);
        gd_mta.showDialog();
        if (gd_mta.wasCanceled()) {
            return;
        }
        boolean agreed = gd_mta.getNextBoolean();
        if (!agreed) return;
        setShowMTA(false);
    }

    public void setShowMTA(boolean show) {
        prefs.set("NJ.showMTA", show);
        prefs.savePreferences();
    }

}
