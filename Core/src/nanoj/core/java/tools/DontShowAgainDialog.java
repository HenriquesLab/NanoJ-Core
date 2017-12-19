package nanoj.core.java.tools;

import ij.gui.NonBlockingGenericDialog;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 24/11/2015
 * Time: 18:56
 */
public class DontShowAgainDialog {

    private static Prefs prefs = new Prefs();

    public static boolean dontShowAgainDialog(String prefsLabel, String title, String msg) {
        if (prefs.getDontShowAgain(prefsLabel)) return true;

        NonBlockingGenericDialog gd = new NonBlockingGenericDialog(title);
        gd.addMessage(msg);
        gd.addCheckbox("Don't show again...", false);
        gd.showDialog();

        if (gd.wasCanceled()) return false;
        boolean dontShowAgain = gd.getNextBoolean();
        prefs.setDontShowAgain(prefsLabel, dontShowAgain);

        return true;
    }
}
