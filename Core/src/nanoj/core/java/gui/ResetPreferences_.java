package nanoj.core.java.gui;

import ij.plugin.PlugIn;
import nanoj.core.java.tools.Log;
import nanoj.core.java.tools.Prefs;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/01/15
 * Time: 13:47
 */
public class ResetPreferences_ implements PlugIn {

    Prefs prefs = new Prefs();
    Log log = new Log();

    @Override
    public void run(String s) {
        prefs.resetPreferences();
        log.msg("Please restart ImageJ without any further action...");
    }
}

