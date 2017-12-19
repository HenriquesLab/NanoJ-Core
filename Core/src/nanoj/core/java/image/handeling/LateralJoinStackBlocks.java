package nanoj.core.java.image.handeling;

import ij.ImageStack;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 24/03/15
 * Time: 16:57
 */
public class LateralJoinStackBlocks {
    private final int width, height;
    private ImageStack ims;

    public LateralJoinStackBlocks(int width, int height) {
        this.width = width;
        this.height = height;
        ims = new ImageStack(width, height);
    }

    public void addBlock(ImageStack imsBlock, int positionX, int positionY) {
        if (ims.getSize() == 0) {
            for (int n=1; n<=imsBlock.getSize(); n++) {
                if (imsBlock.getBitDepth() == 16)
                    ims.addSlice(new ShortProcessor(width, height));
                else
                    ims.addSlice(new FloatProcessor(width, height));
                ims.setSliceLabel(imsBlock.getSliceLabel(n), n);
            }
        }

        ImageProcessor ipBlock;

        for (int n=1; n<=imsBlock.getSize(); n++) {
            ipBlock = imsBlock.getProcessor(n);
            ims.getProcessor(n).copyBits(ipBlock, positionX, positionY, Blitter.COPY);
        }
    }

    public ImageStack getImageStack() {
        return ims;
    }

    public void clear() {
        ims = new ImageStack(width, height);
    }

}
