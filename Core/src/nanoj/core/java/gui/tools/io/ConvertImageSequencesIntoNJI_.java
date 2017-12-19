package nanoj.core.java.gui.tools.io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.array.ArrayCasting;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.tools.MapTools;
import nanoj.core.java.tools.Options;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 29/04/15
 * Time: 13:11
 */
public class ConvertImageSequencesIntoNJI_ extends _BaseDialog_ {
    String dirPath;
    int extension;
    String extensionString;

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
    }

    @Override
    public boolean loadSettings() {
        extension = MapTools.getKeyByValue(Options.extensionChoice, gd.getNextChoice());
        extensionString = Options.extensionChoice.get(extension);
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        ImageStack imsOut = null;
        String title = "";

        File directory = new File(dirPath);
        if(!directory.exists()){
            return;
        }
        File[] listOfFiles = directory.listFiles();
        Arrays.sort(listOfFiles);

        for (int i=0;i<listOfFiles.length;i++){
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.progress(i+1, listOfFiles.length);

            if(listOfFiles[i].isFile() && listOfFiles[i].toString().endsWith(extensionString)) {
                String fPath = listOfFiles[i].getPath();
                if (new File(fPath.replace(extensionString, "-000.nji")).exists()) continue;

                IJ.run("Bio-Formats Windowless Importer", "open=["+fPath+"]");
                ImagePlus imp = IJ.getImage();

                SaveStackAsNJI_ sNJI = new SaveStackAsNJI_();
                sNJI.filePath = fPath.replace(extensionString, ".nji");
                sNJI.run();
                imp.close();
            }
        }
    }
}
