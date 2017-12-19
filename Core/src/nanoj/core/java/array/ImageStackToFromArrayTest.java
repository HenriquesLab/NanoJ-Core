package nanoj.core.java.array;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/4/13
 * Time: 10:34 AM
 */
public class ImageStackToFromArrayTest {

    ImagePlus imp;
    Random random = new Random();

    @Before
    public void setUp() throws Exception {

        int width = 64;
        int height = 64;
        float[] pixels = new float[width*height*100];
        for (int i = 0; i<pixels.length; i++){
            pixels[i] = i;
        }
        ImageStack ims = new ImageStack(width, height);
        int widthHeight = width*height;
        int depth = pixels.length/widthHeight;

        float [] _pixels;

        for (int n=0; n< depth; n++)
        {
            _pixels = new float[widthHeight];
            System.arraycopy(pixels, n*widthHeight, _pixels, 0, widthHeight);
            ims.addSlice(new FloatProcessor(width, height, _pixels));
        }

        imp = new ImagePlus("Test",ims);
    }

    @After
    public void tearDown() throws Exception {
        imp.close();
    }

    @Test
    public void testImageStackToAndFromFloatArray() throws Exception {

        int v1 = random.nextInt(imp.getNSlices()-1)+1;
        int v2 = random.nextInt(imp.getNSlices()-1)+1;
        int start = min(v1, v2);
        int stop  = max(v1, v2);

        float [] data = ImageStackToFromArray.ImageStackToFloatArray(imp.getStack(), start, stop);
        assert data.length == imp.getWidth()*imp.getHeight()*(stop-start);

        ImageStack ims = ImageStackToFromArray.ImageStackFromFloatArray(data, imp.getWidth(), imp.getHeight());

        //imp.show();
        //new ImagePlus("test2", ims).show();
        //sleep(10000);

        int n = random.nextInt(stop - start);
        //n = min(n, imp.getNSlices());

        //new ImagePlus("test1", imp.getStack().getProcessor(start+n-1)).show();
        //new ImagePlus("test2", ims.getProcessor(n)).show();
        //sleep(10000);

        assert ims.getProcessor(n).getStatistics().mean == imp.getStack().getProcessor(start+n-1).getStatistics().mean;


    }
}
