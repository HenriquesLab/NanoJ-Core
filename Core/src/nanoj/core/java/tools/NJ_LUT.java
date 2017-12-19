package nanoj.core.java.tools;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.LUT;

import java.io.File;
import java.io.IOException;

import static nanoj.updater.java.NativeTools.getLocalFileFromResource;

/**
 * Created by paxcalpt on 05/06/2017.
 */
public class NJ_LUT {

    public static void applyLUT(ImagePlus imp, String path) {
        File temp = null;
        try {
            temp = getLocalFileFromResource("/"+path);
        } catch (IOException e) {
            IJ.log("Couldn't find resource: "+path);
        }
        if (temp != null) {
            LUT lut = new LutLoader().openLut(temp.getAbsolutePath());
            imp.setLut(lut);
        }
    }

    public static void applyLUT_NanoJ_Orange(ImagePlus imp) {
        applyLUT(imp,"NanoJ-Orange.lut");
    }

    public static void applyLUT_SQUIRREL_Errors(ImagePlus imp) {
        applyLUT(imp,"SQUIRREL-Errors.lut");
    }

    public static void applyLUT_SQUIRREL_FRC(ImagePlus imp) {
        applyLUT(imp,"SQUIRREL-FRC.lut");
    }
}
