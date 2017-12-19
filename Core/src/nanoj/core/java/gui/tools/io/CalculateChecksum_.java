package nanoj.core.java.gui.tools.io;

import ij.IJ;
import nanoj.core.java.gui._BaseDialog_;

import java.io.IOException;

import static nanoj.updater.java.Path.getCRC32;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/04/15
 * Time: 14:17
 */
public class CalculateChecksum_ extends _BaseDialog_ {

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = false;
        return true;
    }

    @Override
    public void setupDialog() {

    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        String path = IJ.getFilePath("File to calculate MD5 on...");
        log.msg("Calculating checksum..."+
                "\nFile: "+path+
                "\nCRC32: "+getCRC32(path)+
                "\n");
    }
}
