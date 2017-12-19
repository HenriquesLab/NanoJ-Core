package nanoj.core.java.io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 12/02/15
 * Time: 19:12
 */
public class ThreadedImageSave {
    private String path;
    private String prefix = "img";
    private String postfix = "";
    private String extension = ".tif";
    private List<ThreadSaver> threadSaver = new LinkedList<ThreadSaver>();
    static public int bufferSize = 2;
    public boolean saveIndividualFilesFromStack = true;

    public void setPath(String path){
        this.path = path;
    }

    public void setPrefix(String prefix){
        this.prefix = prefix;
    }

    public void setPostfix(String postfix){
        this.postfix = postfix;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    synchronized private void wait4buffer() {
        if (threadSaver.size()>= bufferSize) this.join();
    }

    synchronized public void save(FloatProcessor fp, int imageIndex) {
        wait4buffer();
        ThreadSaver ts = new ThreadSaver();
        ts.setup(new ImagePlus("", fp), imageIndex, path);
        ts.start();
        threadSaver.add(ts);
    }

    synchronized public void save(ImagePlus imp, int imageIndex) {
        wait4buffer();
        ThreadSaver ts = new ThreadSaver();
        ts.setup(imp, imageIndex, path);
        ts.start();
        threadSaver.add(ts);
    }

    synchronized public void join() {
        for (ThreadSaver t: threadSaver) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        threadSaver.clear();
    }

    class ThreadSaver extends Thread {

        private String path;
        private int n;
        private ImagePlus imp;

        public void setup(ImagePlus imp, int imageIndex, String path) {
            this.path = path;
            this.n = imageIndex;
            this.imp = imp;
        }

        @Override
        public void run() {
            int nSlices = imp.getNSlices();
            if (!saveIndividualFilesFromStack || nSlices == 1)
                IJ.save(imp, path + prefix + String.format("%09d", n) + postfix + extension);
            else {
                ImageStack ims = imp.getImageStack();
                for (int s=0;s<ims.getSize();s++) {
                    IJ.save(new ImagePlus("", ims.getProcessor(s+1)),
                            path + prefix + String.format("%09d", n) + "-" + String.format("%06d", s) + postfix + extension);
                }
            }
        }
    }
}

