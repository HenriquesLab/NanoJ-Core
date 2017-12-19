package nanoj.core.java.io.zip.threadedSave;

import ij.ImageStack;
import nanoj.core.java.io.zip.SaveFileInZip;
import nanoj.core.java.io.zip.imageInBlocks.SaveImageByBlocksInZip;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 12/02/15
 * Time: 19:12
 */
public class ThreadedImageSaveByBlocksInZip extends _BaseThreadedImageSave_ {

    private SaveImageByBlocksInZip saveImageByBlocksInZip;
    private boolean bufferSet = false;

    public ThreadedImageSaveByBlocksInZip(String filePath) {
        try {
            saveImageByBlocksInZip = new SaveImageByBlocksInZip(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMagnification(int magnification) {
        this.saveImageByBlocksInZip.setMagnification(magnification);
    }

    public SaveFileInZip getSaveInZipFileClass() {
        return saveImageByBlocksInZip.getSaveInZipFileClass();
    }

    synchronized public void save(ImageStack ims, int t) {
        if (!bufferSet) {
            updateBufferSize(ims);
            bufferSet = true;
        }
        wait4buffer();
        ThreadByBlocksSaver ts = new ThreadByBlocksSaver();
        ts.setup(saveImageByBlocksInZip, ims, t);
        ts.start();
        threadSaver.add(ts);
    }

    synchronized public void close() {
        join();
        try {
            saveImageByBlocksInZip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ThreadByBlocksSaver extends Thread {

    SaveImageByBlocksInZip saveImageByBlocksInZip;
    ImageStack ims;
    int t;

    public void setup(SaveImageByBlocksInZip saveImageByBlocksInZip, ImageStack ims, int t) {
        this.saveImageByBlocksInZip = saveImageByBlocksInZip;
        this.ims = ims;
        this.t = t;
    }

    @Override
    public void run() {
        try {
            ImageStack ims = this.ims;
            for (int n=0; n<ims.getSize(); n++) {
                saveImageByBlocksInZip.addFrame(ims.getProcessor(n+1), "img", n, t);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
