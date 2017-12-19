package nanoj.core.java.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.FolderOpener;
import ij.plugin.PlugIn;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.io.zip.virtualStacks.FullFramesVirtualStack;

import java.io.File;
import java.io.IOException;

/**
 * Created by paxcalpt on 02/02/15.
 */
public class _BaseTestWithPath_ implements PlugIn {

    public Prefs prefs = new Prefs();
    public ImagePlus imp = null;

    private String path;
    private boolean get_pathQ;

    public void loadImages(){
        //load ImageJ preferences and get previously used directory

        path = Prefs.get("NJ.Common.test_image_path", "");

        //set up dialog to change directory if needed
        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Update stuff for testing?");
        gd.addCheckbox("Update location of image file?", false);

        // Show dialog
        gd.showDialog();
        if (gd.wasCanceled()) return;

        //get user choice
        get_pathQ = gd.getNextBoolean();

        //if chosen get new directory
        if (get_pathQ || path.equals("")) {
            setupImp();
        }

        if(imp == null) return;

        //save directory used in preferences
        prefs.set("NJ.Common.test_image_path", path);
        prefs.savePreferences();
    }

    @Override
    public void run(String s) {
        if (CLDevice.chosenDevice == null){
            CLDevice.setChosenDevice();
        }
    }

    private void setupImp(){

        if(get_pathQ){
            openImpAndGetPath();
        }else {
            openImpWithPath();
        }

        if(imp == null) return;

        if (imp.isComposite())
            IJ.log("WARNING: detected composite image. NanoJ is optimized for pure image stacks and may yield errors.");
        if (imp.isHyperStack())
            IJ.log("WARNING: detected hyperstack. NanoJ is optimized for pure image stacks and may yield errors.");
    }

    private void openImpWithPath(){
        String[] dataTypes = {"NJ.zip File", "Tif Image Sequence", "Tiff Stack"};
        String dataType;
        File f = new File(path);

        if(f.exists() && f.isDirectory()){
            dataType = dataTypes[1];
        }else if(f.exists() && !f.isDirectory() && path.substring(path.length() - 3).equals("zip")){
            dataType = dataTypes[0];
        }else if(f.exists()){
            dataType = dataTypes[2];
        }else{
            return;
        }


        if(dataType.equals(dataTypes[0])){

            //open NJ.zip file selected and show
            FullFramesVirtualStack mzvs = null;
            String impTitle = "Opened Data";
            try {
                mzvs = new FullFramesVirtualStack(path);
                impTitle = mzvs.getTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            imp = new ImagePlus(impTitle, mzvs);

            imp.show();

        }else if(dataType.equals(dataTypes[1])){

            //open image sequence in directory selected and show
            imp = FolderOpener.open(path);

            if(imp == null) return;
            imp.show();

        }else if(dataType.equals(dataTypes[2])){

            //Get path and use imageJ opener
            IJ.open(path);
            imp = IJ.getImage();
        }

    }

    private void openImpAndGetPath(){

        String[] dataTypes = {"NJ.zip File", "Tif Image Sequence", "Tiff Stack"};

        NonBlockingGenericDialog gdImpOpen = new NonBlockingGenericDialog("Data Type");

        gdImpOpen.addRadioButtonGroup("Data Type", dataTypes,3,1,dataTypes[0]);

        gdImpOpen.showDialog();
        if (gdImpOpen.wasCanceled()) {
            return;
        }

        String dataType = gdImpOpen.getNextRadioButton();

        if(dataType.equals(dataTypes[0])){

            //open NJ.zip file selected and show
            path = IJ.getFilePath("Choose data to load...");
            FullFramesVirtualStack mzvs = null;
            String impTitle = "Opened Data";
            try {
                mzvs = new FullFramesVirtualStack(path);
                impTitle = mzvs.getTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            imp = new ImagePlus(impTitle, mzvs);

            imp.show();

        }else if(dataType.equals(dataTypes[1])){

            //open image sequence in directory selected and show
            path = IJ.getDirectory("Choose image sequence to analyse...");
            imp = FolderOpener.open(path);

            if(imp == null) return;
            imp.show();

        }else {

            //Get path and use imageJ opener
            path = IJ.getFilePath("Choose data to load...");
            IJ.open(path);
            imp = IJ.getImage();
        }
    }
}
