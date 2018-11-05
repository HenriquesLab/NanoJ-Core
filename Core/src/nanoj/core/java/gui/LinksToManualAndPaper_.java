package nanoj.core.java.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

import static nanoj.updater.java.NativeTools.getLocalFileFromResource;

/**
 * Created by sculley on 05/11/2018.
 */
public class LinksToManualAndPaper_ implements PlugIn {

    @Override
    public void run(String s) {
        try{
            File temp = getLocalFileFromResource("/About_NanoJ-Core.png");
            ImagePlus imp = IJ.openImage(temp.getAbsolutePath());
            imp.show();
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

}
