package nanoj.core.java.io;

import ij.ImageStack;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ThreadedPartitionData {

    private ImageStack imsOriginal;

    private int nSlices, w, h;
    private int xBlockSize, yBlockSize, tBlockSize, xBlockBorder, yBlockBorder;
    public int xTotalBlocks, yTotalBlocks, xyTotalBlocks, tTotalBlocks;
    private int nBlockXY = 0;
    private int nBlockT = 0;

    ThreadedTimeBlockLoader currentBlockThread, nextBlockThread;

    public ThreadedPartitionData(ImageStack imsOriginal){
        this.imsOriginal = imsOriginal;
        nSlices = imsOriginal.getSize();
        w = imsOriginal.getWidth();
        h = imsOriginal.getHeight();
    }

    public void setupBlockSize(int tBlockSize){
        this.xBlockSize = w;
        this.yBlockSize = h;
        this.xBlockBorder = 0;
        this.yBlockBorder = 0;
        this.tBlockSize = tBlockSize;
        this.xTotalBlocks = 1;
        this.yTotalBlocks = 1;
        this.xyTotalBlocks = 1;
        this.tTotalBlocks = nSlices % tBlockSize==0? nSlices / tBlockSize: nSlices / tBlockSize + 1;

        currentBlockThread = threadedLoadTimeBlock();
        nextBlockThread = threadedLoadTimeBlock();
    }

    public void setupBlockSize(int xBlockSize, int yBlockSize, int tBlockSize, int xBlockBorder, int yBlockBorder) {
        this.xBlockSize = min(xBlockSize, w);
        this.yBlockSize = min(yBlockSize, h);
        this.xBlockBorder = xBlockBorder;
        this.yBlockBorder = yBlockBorder;
        this.tBlockSize = tBlockSize;
        this.xTotalBlocks = w % xBlockSize == 0? w / xBlockSize: w / xBlockSize + 1;
        this.yTotalBlocks = h % yBlockSize == 0? h / yBlockSize: h / yBlockSize + 1;
        this.xyTotalBlocks = xTotalBlocks * yTotalBlocks;
        this.tTotalBlocks = nSlices % tBlockSize==0? nSlices / tBlockSize: nSlices / tBlockSize + 1;

        currentBlockThread = threadedLoadTimeBlock();
        nextBlockThread = threadedLoadTimeBlock();
    }

    private ThreadedTimeBlockLoader threadedLoadTimeBlock() {
        if (nBlockT == tTotalBlocks) return null;
        int tStart = nBlockT * tBlockSize + 1;
        int tEnd = min((nBlockT +1) * tBlockSize, nSlices);
        ThreadedTimeBlockLoader tl = new ThreadedTimeBlockLoader();
        tl.setup(imsOriginal, tStart, tEnd);
        tl.start();
        nBlockT++;
        return tl;
    }

    public ImageStack getNextBlock() {

        try {
            if (currentBlockThread.isAlive()) currentBlockThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ImageStack ims = currentBlockThread.imsBlocks[nBlockXY];
        if (nBlockXY == xyTotalBlocks - 1) {
            currentBlockThread = nextBlockThread;
            nextBlockThread = threadedLoadTimeBlock();
            nBlockXY = 0;
        }
        else nBlockXY++;

        return ims;
    }

    class ThreadedTimeBlockLoader extends Thread {
        private ImageStack imsOriginal;
        protected ImageStack[] imsBlocks;
        private int sliceStart;
        private int sliceEnd;

        public void setup(ImageStack imsOriginal, int sliceStart, int sliceEnd) {
            this.imsOriginal = imsOriginal;
            this.sliceStart = sliceStart;
            this.sliceEnd = sliceEnd;
        }

        @Override
        public void run() {
            ImageStack imsBlock = new ImageStack(imsOriginal.getWidth(), imsOriginal.getHeight());
            imsBlocks = new ImageStack[xyTotalBlocks];

            for (int n = sliceStart; n <= sliceEnd; n++) {
                imsBlock.addSlice(imsOriginal.getProcessor(n));
            }

            int counter = 0;
            for (int yB = 0; yB<yTotalBlocks; yB++) {
                for (int xB=0; xB<xTotalBlocks; xB++) {
                    int xStart = max(xB * xBlockSize - xBlockBorder, 0);
                    int xEnd = min((xB + 1) * xBlockSize + xBlockBorder, w);
                    int yStart = max(yB * yBlockSize - yBlockBorder, 0);
                    int yEnd = min((yB + 1) * yBlockSize + yBlockBorder, h);
                    int w = xEnd - xStart;
                    int h = yEnd - yStart;

                    imsBlocks[counter] = imsBlock.crop(xStart, yStart, 0, w, h, imsBlock.getSize());
                    counter++;
                }
            }
        }
    }
}
