package nanoj.core.java.projections;

import ij.ImageStack;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import static nanoj.core.java.array.ImageStackToFromArray.ImageStackFromFloatArray;
import static nanoj.core.java.array.ImageStackToFromArray.ImageStackToFloatArray;

/**
 * Created by sculley on 30/03/15.
 */
public class Projections3D {

    private static Log log = new Log();

    public static ImageStack findAverageZKernel(ImageStack ims, int framesPerGroup, int zb) {
        Kernel_group3DProject kernel_group3DProject = new Kernel_group3DProject();
        float[] pixels = ImageStackToFloatArray(ims);
        pixels = kernel_group3DProject.doProjection(pixels, ims.getWidth(), ims.getHeight(), framesPerGroup, zb);
        return ImageStackFromFloatArray(pixels, ims.getWidth(), ims.getHeight());
    }
}

class Kernel_group3DProject extends NJKernel {

    private int width, height, widthHeight, zKernel, framesPerGroup, nFramesOriginal, nGroups;
    private float[] pixelsProjection, pixelsOriginal;

    public float[] doProjection(float[] pixels, int width, int height, int framesPerGroup, int zb) {

        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.zKernel = zb;

        this.framesPerGroup = framesPerGroup;
        this.nFramesOriginal = pixels.length / widthHeight;

        this.nGroups = nFramesOriginal / framesPerGroup;
        if (framesPerGroup * nGroups != nFramesOriginal) this.nGroups++;

        this.pixelsOriginal = pixels;
        this.pixelsProjection = new float[widthHeight * nGroups];

        autoChooseDeviceForNanoJ();

        execute(this.widthHeight * nGroups);

        return pixelsProjection;
    }

    @Override
    public void run() {
        int p, x, y, g;

        p = getGlobalId(0);
        x = p % (width);
        y = (p / width) % height;
        g = p / (widthHeight);

        int tStart = 0;
        int tEnd = 0;

        tStart = max(g - framesPerGroup / 2, 0);
        tEnd = min(g + framesPerGroup / 2, nFramesOriginal);

        int nTsInGroup = tEnd - tStart;

        float vSum = 0;
        float vWeightedSum = 0;
        for (int t = tStart; t < tEnd; t++) {
            vSum += pixelsOriginal[getIdxO(x, y, t)];
            vWeightedSum += pixelsOriginal[getIdxO(x, y, t)] * zKernel;
        }

        pixelsProjection[getIdxP(x, y, g)] = vWeightedSum / vSum;

    }

    private int getIdxP(int x, int y, int g) { // idx in projection
        int pg = g * widthHeight; // position in time * pixels in a frame
        int pf = y * width + x; // position within a frame
        return pg + pf;
    }

    private int getIdxO(int x, int y, int t) { // idx in original
        int pt = t * widthHeight; // position in time * pixels in a frame
        int pf = y * width + x; // position within a frame
        return pt + pf;
    }

}