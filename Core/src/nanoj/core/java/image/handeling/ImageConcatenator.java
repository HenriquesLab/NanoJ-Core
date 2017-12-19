package nanoj.core.java.image.handeling;

import ij.ImagePlus;
import ij.ImageStack;

/**
 * Created by paxcalpt on 25/01/15.
 */
public class ImageConcatenator {

    public static void concatenate(ImagePlus imp1, ImagePlus imp2) {
        ImageStack ims1 = imp1.getImageStack();
        ImageStack ims2 = imp2.getImageStack();
        concatenate(ims1, ims2);
    }

    public static void concatenate(ImageStack ims1, ImageStack ims2) {
        for (int n=1;n<=ims2.getSize();n++){
            ims1.addSlice(ims2.getProcessor(n));
            ims1.setSliceLabel(ims2.getSliceLabel(n), ims1.getSize());
        }
    }
}
