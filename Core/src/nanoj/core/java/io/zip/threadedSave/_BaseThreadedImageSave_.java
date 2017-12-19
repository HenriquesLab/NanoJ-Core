package nanoj.core.java.io.zip.threadedSave;

import ij.ImageStack;
import nanoj.core.java.imagej.ImagePlusTools;
import nanoj.core.java.io.zip.SaveFileInZip;
import nanoj.core.java.tools.Log;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.*;
import static java.lang.Thread.sleep;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/04/15
 * Time: 20:16
 */
public abstract class _BaseThreadedImageSave_ {

    protected Log log = new Log();

    private Runtime rt = Runtime.getRuntime();
    public int bufferSize = rt.availableProcessors();
    protected List<Thread> threadSaver = new LinkedList<Thread>();

    synchronized public void updateBufferSize(ImageStack ims) {
        long impMemory = ImagePlusTools.getMemorySizeBytes(ims);
        long availableMemory = (long) (rt.maxMemory()*0.5);
        bufferSize = (int) min(max(abs(availableMemory / impMemory), 1), 100);
        log.msg(2, String.format("%.2gMB RAM available each block uses %.2gMB", availableMemory/1e6, impMemory/1e6));
        log.msg(2, "updated threaded image saver buffer to use "+bufferSize+" blocks");
    }

    synchronized protected void wait4buffer() {
        clearFinishedThreads();
        if (threadSaver.size()>= bufferSize) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wait4buffer();
        }
    }

    synchronized public void join() {
        log.status("waiting for data to save, "+threadSaver.size()+" blocks to go...");
        clearFinishedThreads();
        if (threadSaver.size()> 0) {
            try {
                if (threadSaver.size()>=100) sleep(30000);
                else if (threadSaver.size()>=50) sleep(10000);
                else if (threadSaver.size()>=20) sleep(1000);
                else if (threadSaver.size()>=10) sleep(100);
                else if (threadSaver.size()>=5) sleep(10);
                else sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            join();
        }
        else {
            log.status("done...");
        }
    }

    synchronized private void clearFinishedThreads() {
        int counter = 0;
        while (counter<threadSaver.size()) {
            if (!threadSaver.get(counter).isAlive()) threadSaver.remove(counter);
            else counter++;
        }
    }

    abstract public void close();

    abstract public SaveFileInZip getSaveInZipFileClass();
}
