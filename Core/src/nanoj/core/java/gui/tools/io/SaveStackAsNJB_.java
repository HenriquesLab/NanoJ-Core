package nanoj.core.java.gui.tools.io;

import ij.ImageStack;
import ij.io.SaveDialog;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.io.zip.imageInBlocks.SaveImageByBlocksInZip;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/03/15
 * Time: 21:32
 */
public class SaveStackAsNJB_ extends _BaseDialog_ {
    private String filePath;

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = true;
        return true;
    }

    @Override
    public void setupDialog(){
        SaveDialog sd = new SaveDialog(
                "Choose where to save dataset...",
                prefs.get("NJ.defaultSavePath", ""),
                imp.getTitle(), ".njb");
        if (sd.getFileName() == null) {
            return;
        }
        filePath = sd.getDirectory()+sd.getFileName();
        prefs.set("NJ.defaultSavePath", sd.getDirectory());
        prefs.set("NJ.filePath", filePath);
    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        SaveImageByBlocksInZip saveImageByBlocksInZip = new SaveImageByBlocksInZip(filePath);
        ImageStack ims = imp.getImageStack();
        int nSlices = ims.getSize();

        for (int n=1; n<=nSlices; n++) {
            log.progress(n, nSlices);
            log.status("saving "+n+"/"+nSlices+"...");

            saveImageByBlocksInZip.addFrame(ims.getProcessor(n), "img", 0, n-1);
        }
        saveImageByBlocksInZip.close();
        log.status("Done...");
    }
}
