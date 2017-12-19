package nanoj.core.java.io.zip.threadedSave;

import ij.ImagePlus;
import ij.ImageStack;
import nanoj.core.java.io.zip.SaveFileInZip;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 12/02/15
 * Time: 19:12
 */
public class ThreadedImageSaveInZip extends _BaseThreadedImageSave_ {

    private SaveFileInZip saveFileInZip;

    public ThreadedImageSaveInZip(String filePath, int compression) {
        try {
            saveFileInZip = new SaveFileInZip(filePath, true);
            saveFileInZip.setLevel(compression);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SaveFileInZip getSaveInZipFileClass() {
        return saveFileInZip;
    }

    synchronized public void save(ImagePlus imp, int z, int t) {
        wait4buffer();
        ThreadSaver ts = new ThreadSaver();
        ts.setup(saveFileInZip, imp, z, t);
        ts.start();
        threadSaver.add(ts);
    }

    synchronized public void close() {
        join();
        try {
            saveFileInZip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ThreadSaver extends Thread {

    SaveFileInZip saveFileInZip;
    ImagePlus imp;
    int z, t;

    public void setup(SaveFileInZip saveFileInZip, ImagePlus imp, int z, int t) {
        this.saveFileInZip = saveFileInZip;
        this.imp = imp;
        this.z = z;
        this.t = t;
    }

    @Override
    public void run() {
        String title;
        try {
            ImageStack ims = imp.getImageStack();
            for (int n=0; n<ims.getSize(); n++) {
                title = "img_"+"z"+z+"t"+t+".tif";
                this.saveFileInZip.addTiffImage(title, new ImagePlus(title, ims.getProcessor(n+1)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
