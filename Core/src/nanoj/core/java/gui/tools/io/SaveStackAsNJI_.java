package nanoj.core.java.gui.tools.io;

import ij.ImagePlus;
import ij.ImageStack;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.io.zip.SaveFileInZip;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static nanoj.core.java.imagej.FilesAndFoldersTools.getSavePath;
import static nanoj.core.java.io.zip.SaveFileInZip.convertSliceLabelIntoNiceTiffName;

/**
 * Created by paxcalpt on 08/03/15.
 */
public class SaveStackAsNJI_ extends _BaseDialog_ {

    public String filePath;

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = true;
        return true;
    }

    @Override
    public void setupDialog(){
    }

    @Override
    public boolean loadSettings() {
        if (filePath == null) {
            filePath = getSavePath("Choose where to save dataset...", imp.getTitle(), ".nji");
            if (filePath == null) return false;
        }
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        ImagePlus impFrame;
        ImageStack ims = imp.getImageStack();

        SaveFileInZip saveFileInZip = new SaveFileInZip(filePath, true);
        String filename = new File(filePath).getName();

        int nSlices = ims.getSize();

        for (int n=1; n<=nSlices; n++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.progress(n, nSlices);
            log.status("saving "+n+"/"+nSlices+": "+filename);

            String imageName = convertSliceLabelIntoNiceTiffName(ims, n);
            impFrame = new ImagePlus("", ims.getProcessor(n));
            if (n==1) {
                impFrame.setCalibration(imp.getCalibration());
                if (imp.getCalibration().getUnit() == "micron")
                    impFrame.getCalibration().setUnit("um");
                else
                    impFrame.getCalibration().setUnit(imp.getCalibration().getUnit());
                //impFrame.getCalibration().setTimeUnit(imp.getCalibration().getTimeUnit());
                Properties p = imp.getProperties();
                if (p != null) {
                    for (Object key : p.keySet())
                        impFrame.setProperty((String) key, p.get(key));
                }
                impFrame.setFileInfo(imp.getOriginalFileInfo());
                impFrame.setOverlay(imp.getOverlay());
                impFrame.setRoi(imp.getRoi());
//                String info = new Info().getImageInfo(impFrame, impFrame.getProcessor());
//                saveFileInZip.addText("info.txt", info);
            }
            saveFileInZip.addTiffImage(imageName, impFrame);
        }

        saveFileInZip.close();
        log.status("Done...");
    }
}
