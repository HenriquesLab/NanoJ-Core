package nanoj.core.java.gui.tools.io;

import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.gui._BaseDialog_;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 02/04/15
 * Time: 16:54
 */
public class SetCompression_ extends _BaseDialog_ {

    int compression;

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = false;
        return true;
    }

    @Override
    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Set compression level for NJB and NJI files...");
        gd.addSlider("Compression level", 0, 9, prefs.getCompressionLevel());
    }

    @Override
    public boolean loadSettings() {
        compression = (int) gd.getNextNumber();
        if (compression < 0 || compression > 9) return false;
        prefs.setCompressionLevel(compression);
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

    }
}
