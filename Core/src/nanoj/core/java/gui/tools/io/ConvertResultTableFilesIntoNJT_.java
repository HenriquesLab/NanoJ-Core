package nanoj.core.java.gui.tools.io;

import ij.IJ;
import ij.ImageStack;
import ij.measure.ResultsTable;
import nanoj.core.java.gui._BaseDialog_;

import java.io.File;
import java.io.IOException;

import static nanoj.core.java.io.SaveNanoJTable.saveNanoJTable;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 31/01/16
 * Time: 18:26
 */
public class ConvertResultTableFilesIntoNJT_ extends _BaseDialog_ {
    String dirPath;

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
    }

    @Override
    public boolean loadSettings() {
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

        for (int i=0;i<listOfFiles.length;i++){
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.progress(i+1, listOfFiles.length);

            if(listOfFiles[i].isFile() && listOfFiles[i].toString().endsWith(".xls")) {
                String fPath = listOfFiles[i].getPath();

                ResultsTable rt = ResultsTable.open(fPath);
                saveNanoJTable(fPath.replace(".xls", ".njt"), rt);
            }
        }
    }
}
