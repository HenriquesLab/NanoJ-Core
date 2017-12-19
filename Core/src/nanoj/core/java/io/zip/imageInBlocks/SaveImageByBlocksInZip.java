package nanoj.core.java.io.zip.imageInBlocks;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import nanoj.core.java.io.zip.SaveFileInZip;

import java.io.IOException;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by paxcalpt on 15/03/15.
 */
public class SaveImageByBlocksInZip {

    private String filePath;

    private int width, height;
    private int blockWidth = 32;
    private int blockHeight = 32;
    private int blockWidthMargin = 2;
    private int blockHeightMargin = 2;
    private int maxXBlock = 0;
    private int maxYBlock = 0;
    private int maxZBlock = 0;
    private int maxTBlock = 0;
    private int magnification = 0;
    private boolean blockSizeSet = false;
    SaveFileInZip saveFileInZip = null;

    public SaveImageByBlocksInZip(String filePath) throws IOException {
        if (!filePath.endsWith(".njb")) filePath += ".njb";

        this.filePath = filePath;
        this.saveFileInZip = new SaveFileInZip(filePath, true);
    }

    public SaveFileInZip getSaveInZipFileClass() {
        return saveFileInZip;
    }

    public void setMagnification(int magnification) {
        this.magnification = magnification;
    }

    synchronized public void setBlockSize(int blockWidth, int blockHeight, int blockWidthMargin, int blockHeightMargin) {
        assert (!blockSizeSet); // allow only one run
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        this.blockWidthMargin = blockWidthMargin;
        this.blockHeightMargin = blockHeightMargin;
        blockSizeSet = true;
    }

    synchronized public void copySettings(OpenImageFromBlocksInZip openImageFromBlocksInZip,
                                          boolean withMargins, boolean resetMax) {
        width = openImageFromBlocksInZip.width;
        height = openImageFromBlocksInZip.height;
        blockWidth = openImageFromBlocksInZip.blockWidth;
        blockHeight = openImageFromBlocksInZip.blockHeight;
        if (withMargins) {
            blockWidthMargin = openImageFromBlocksInZip.blockWidthMargin;
            blockHeightMargin = openImageFromBlocksInZip.blockHeightMargin;
        }
        else {
            blockWidthMargin = 0;
            blockHeightMargin = 0;
        }
        if (!resetMax) {
            maxXBlock = openImageFromBlocksInZip.maxXBlock;
            maxYBlock = openImageFromBlocksInZip.maxYBlock;
            maxZBlock = openImageFromBlocksInZip.maxZBlock;
            maxTBlock = openImageFromBlocksInZip.maxTBlock;
        }
        magnification = openImageFromBlocksInZip.magnification;
        blockSizeSet = true;
    }

    synchronized public void setWidthAndHeight(int width, int height, int blockWidth, int blockHeight) {
        this.width = width;
        this.height = height;
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
    }

    synchronized public void addFrame(ImageProcessor ip, String label, int z, int t) throws IOException {
        int xStart, xEnd, yStart, yEnd, roiWidth, roiHeight;
        String pathBlock, positionBlock;
        ImageProcessor ipBlock;

        width = ip.getWidth();
        height = ip.getHeight();

        int xBlocks = width / blockWidth;
        if (width % blockWidth != 0) xBlocks++;
        int yBlocks = height / blockHeight;
        if (height % blockHeight != 0) yBlocks++;

        maxXBlock = max(xBlocks-1, maxXBlock);
        maxYBlock = max(yBlocks-1, maxYBlock);
        maxZBlock = max(z, maxZBlock);
        maxTBlock = max(t, maxTBlock);

        for (int yb=0; yb<yBlocks; yb++) {
            for (int xb=0; xb<xBlocks; xb++) {

                xStart = max((xb * blockWidth) - blockWidthMargin, 0);
                xEnd   = min(((xb+1) * blockWidth) + blockWidthMargin, width);
                yStart = max((yb * blockHeight) - blockHeightMargin, 0);
                yEnd   = min(((yb+1) * blockHeight) + blockHeightMargin, height);
                roiWidth = xEnd - xStart;
                roiHeight = yEnd - yStart;

                pathBlock = label+"_";

                positionBlock = "";
                positionBlock += "x"+xb;//String.format("x%0"+xBlockZFill+"d", xb);
                positionBlock += "y"+yb;//String.format("y%0"+yBlockZFill+"d", yb);
                positionBlock += "z"+z;//String.format("z%06d", z);
                positionBlock += "t"+t;;//String.format("t%06d", t);

                pathBlock = pathBlock + positionBlock;

                ip.setRoi(new Roi(xStart, yStart, roiWidth, roiHeight));
                ipBlock = ip.crop();

                saveFileInZip.addTiffImage(pathBlock + ".tif", new ImagePlus(positionBlock, ipBlock));
                //saveInZipFile.addRawImage(pathBlock + ".raw", new ImagePlus(positionBlock, ipBlock));
            }
        }
    }

    synchronized public void addBlock(ImageStack ims, String label, int z, int xb, int yb) throws IOException {
        String pathBlock, positionBlock;

        maxXBlock = max(xb, maxXBlock);
        maxYBlock = max(yb, maxYBlock);
        maxZBlock = max(z, maxZBlock);
        maxTBlock = max(ims.getSize()-1, maxTBlock);

        for (int t=0; t<ims.getSize(); t++) {
            pathBlock = label+"_";

            positionBlock = "";
            positionBlock += "x"+xb;//String.format("x%0"+xBlockZFill+"d", xb);
            positionBlock += "y"+yb;//String.format("y%0"+yBlockZFill+"d", yb);
            positionBlock += "z"+z;//String.format("z%06d", z);
            positionBlock += "t"+t;;//String.format("t%06d", t);

            pathBlock = pathBlock + positionBlock;

            saveFileInZip.addTiffImage(pathBlock + ".tif", new ImagePlus(positionBlock, ims.getProcessor(t+1)));
        }
    }

    synchronized public void flush() {
        saveFileInZip.flush();
    }

    synchronized public void close() throws IOException {
        String text = "";
        text += "width="+width+"\n";
        text += "height="+height+"\n";
        text += "blockWidth="+blockWidth+"\n";
        text += "blockWidthMargin="+blockWidthMargin+"\n";
        text += "blockHeight="+blockHeight+"\n";
        text += "blockHeightMargin="+blockHeightMargin+"\n";
        text += "maxXBlock="+maxXBlock+"\n";
        text += "maxYBlock="+maxYBlock+"\n";
        text += "maxZBlock="+maxZBlock+"\n";
        text += "maxTBlock="+maxTBlock+"\n";
        text += "magnification="+magnification+"\n";
        saveFileInZip.addText("block-size.txt", text);
        saveFileInZip.close();
    }
}
