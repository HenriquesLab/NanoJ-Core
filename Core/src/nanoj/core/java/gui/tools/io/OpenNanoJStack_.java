package nanoj.core.java.gui.tools.io;

import ij.IJ;
import nanoj.core.java.gui._BaseDialog_;

import java.io.File;
import java.io.IOException;

import static nanoj.core.java.io.OpenNanoJDataset.openNanoJDataset;

/**
 * Created by paxcalpt on 08/03/15.
 */
public class OpenNanoJStack_ extends _BaseDialog_ {

    private String filePath;
    private String arg;

    @Override
    public boolean beforeSetupDialog(String arg) {
        this.arg = arg;
        autoOpenImp = false;
        useSettingsObserver = false;
        return true;
    }

    @Override
    public void setupDialog(){
        if (new File(arg).exists())
            filePath = arg;
        else {
            filePath = IJ.getFilePath("Choose dataset to open...");
            if (filePath == null) return;
        }
    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        if (filePath == null) return;
        imp = openNanoJDataset(filePath);
        if (imp == null) return;
        imp.show();
    }
}
