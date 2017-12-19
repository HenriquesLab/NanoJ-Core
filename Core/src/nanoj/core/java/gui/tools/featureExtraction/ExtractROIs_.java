package nanoj.core.java.gui.tools.featureExtraction;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.frame.RoiManager;
import nanoj.core.java.featureExtraction.ExtractRois;

import java.awt.*;
import java.io.IOException;

/**
 * Created by sculley on 20/05/15.
 */
public class ExtractROIs_ extends nanoj.core.java.gui._BaseDialog_ {
    RoiManager rm;
    String savePath;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = false;

        rm = RoiManager.getInstance();

        if (rm == null || rm.getCount()==0) {
            IJ.error("You will need to have ROIs loaded into the ROI Manager for this to work.");
            return false;
        }

        savePath = IJ.getDirectory("Path to save ROIs in...");
        if (savePath == null) return false;

        return true;
    }

    @Override
    public void setupDialog() {

    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        int nROIs = rm.getCount();

        int[] xStart = new int[nROIs];
        int[] yStart = new int[nROIs];
        int[] rWidth = new int[nROIs];
        int[] rHeight = new int[nROIs];

        for (int n=0; n<nROIs; n++) {
            Rectangle r = rm.getRoisAsArray()[n].getBounds();
            xStart[n] = r.x;
            yStart[n] = r.y;
            rWidth[n] = r.width;
            rHeight[n] = r.height;
        }

        log.status("extracting ROIs...");
        ImageStack ims = imp.getImageStack();
        ImageStack[] imsRois = ExtractRois.extractRois(ims, xStart, yStart, rWidth, rHeight);

        for (int n=0; n<nROIs; n++) {
            String title = imp.getTitle();
            title = title+"_ROI"+n+".nji";

            nanoj.core.java.gui.tools.io.SaveStackAsNJI_ saver = new nanoj.core.java.gui.tools.io.SaveStackAsNJI_();
            saver.filePath = savePath + title;
            saver.imp = new ImagePlus(title, imsRois[n]);
            saver.run();
        }
    }
}
