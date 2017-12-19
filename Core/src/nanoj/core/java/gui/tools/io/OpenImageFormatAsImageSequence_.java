package nanoj.core.java.gui.tools.io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.array.ArrayCasting;
import nanoj.core.java.image.handeling.ImageConcatenator;
import nanoj.core.java.tools.MapTools;
import nanoj.core.java.tools.Options;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.min;
import static nanoj.core.java.io.OpenNanoJDataset.openNanoJDataset;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 29/04/15
 * Time: 13:11
 */
public class OpenImageFormatAsImageSequence_ extends nanoj.core.java.gui._BaseDialog_ {

    public static int EXTENSION_ZEISS_CZI = 0;

    public final static Map<Integer, String> extensionChoice;
    static {
        extensionChoice = new LinkedHashMap<Integer, String>();
        extensionChoice.put(EXTENSION_ZEISS_CZI, ".czi");
    }

    String dirPath;
    int extension;
    String extensionString;
    Boolean checkLength;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = false;
        useSettingsObserver = false;
        return true;
    }

    @Override
    public void setupDialog() {
        dirPath = IJ.getDirectory("Choose directory to open...");
        if (dirPath == null) return;

        gd = new NonBlockingGenericDialog("Choose extension for files in directory...");
        gd.addChoice("Extension",
                ArrayCasting.mapValue2StringArray(Options.extensionChoice),
                Options.extensionChoice.get(getPrefs("extension", Options.EXTENSION_ZEISS_CZI)));
        //gd.addCheckbox("Ensure all image sequences same length?", getPrefs("checkLength", false));
    }

    @Override
    public boolean loadSettings() {
        extension = MapTools.getKeyByValue(Options.extensionChoice, gd.getNextChoice());
        extensionString = Options.extensionChoice.get(extension);

        //checkLength = gd.getNextBoolean();
        //setPrefs("checkLength", checkLength);
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        ImageStack imsOut = null;

        File directory = new File(dirPath);
        if(!directory.exists()){
            return;
        }
        File[] listOfFiles = directory.listFiles();
        Arrays.sort(listOfFiles);

        ArrayList<ImageStack> imsList = new ArrayList<ImageStack>();
        boolean foundVariableSizes = false;
        int previousSize = 0;
        int minSize = Integer.MAX_VALUE;
        String title = null;

        for (int i=0;i<listOfFiles.length;i++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.status("Loading data...");
            log.progress(i + 1, listOfFiles.length);

            if (listOfFiles[i].isFile() && listOfFiles[i].toString().endsWith(extensionString)) {
                if (listOfFiles[i].toString().endsWith(".nji"))
                    openNanoJDataset(listOfFiles[i].getPath()).show();
                else
                    IJ.run("Bio-Formats Windowless Importer", "open=[" + listOfFiles[i].getPath() + "]");

                ImagePlus imp = IJ.getImage();
                ImageStack ims = imp.getImageStack().duplicate();
                imsList.add(ims);
                if (title == null) title = imp.getTitle();
                imp.close();

                if (previousSize == 0) previousSize = ims.getSize();
                else if (previousSize != ims.getSize()) foundVariableSizes = true;
                minSize = min(minSize, ims.getSize());
            }
        }

        boolean doCrop = false;
        if (foundVariableSizes) {
            doCrop = IJ.showMessageWithCancel("Image stacks with variable size",
                    "Images do not all have same number of frames, crop dataset to minimum common number of frames?");
        }


        for (int i=0;i<imsList.size();i++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.status("Concatenating data...");
            log.progress(i + 1, imsList.size());

            ImageStack ims = imsList.get(i);

            while(doCrop && ims.getSize() > minSize){
                ims.deleteLastSlice();
                log.msg("Removing a slice from stack "+i+", stack now has "+ims.getSize()+" frames");
            }

            if(imsOut==null) {
                int width = ims.getWidth();
                int height = ims.getHeight();
                imsOut = new ImageStack(width, height);
            }
            ImageConcatenator.concatenate(imsOut, ims);
            imsList.set(i, null);
        }

        new ImagePlus(title, imsOut).show();
    }
}
