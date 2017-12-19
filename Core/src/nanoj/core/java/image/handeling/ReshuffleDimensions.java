package nanoj.core.java.image.handeling;

import ij.ImageStack;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 10/04/15
 * Time: 10:49
 */
public class ReshuffleDimensions {

    public static ImageStack reshuffleTZtoZT(ImageStack ims, int nTs, int nZs) {
        assert (ims.getSize() == (nTs*nZs));

        ImageStack imsReshuffled = new ImageStack(ims.getWidth(), ims.getHeight());

        for (int t=0;t<nTs;t++) {
            for (int z=0;z<nZs;z++) {
                imsReshuffled.addSlice(ims.getProcessor(z*nTs+t+1));
            }
        }

        return imsReshuffled;
    }
}
