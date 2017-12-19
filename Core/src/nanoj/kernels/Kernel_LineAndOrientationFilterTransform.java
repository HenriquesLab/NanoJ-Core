package nanoj.kernels;

import ij.ImageStack;
import nanoj.core.java.aparapi.NJKernel;

import static nanoj.core.java.array.ImageStackToFromArray.ImageStackFromFloatArray;
import static nanoj.core.java.array.ImageStackToFromArray.ImageStackToFloatArray;

/**
 * Created by Henriques-lab on 20/11/2016.
 */

public class Kernel_LineAndOrientationFilterTransform extends NJKernel {
    // based on the mex file found in:
    // http://www.molbiolcell.org/content/early/2016/11/14/mbc.E16-06-0421.abstract
    // Extracting Microtubule Networks from Superresolution Single-Molecule Localization Microscopy Data
    // Zhen Zhang1,*, Yukako Nishimura1,*, and Pakorn Kanchanawong1,2,#
    // 10.1091/mbc.E16-06-0421

    private final static int STEP_CALCULATE_LFT = 0;
    private final static int STEP_CALCULATE_OFT = 1;

    public float[] pixelsIn, pixelsLFT, pixelsLFTOrientation, pixelsOut;
    private int width, height, widthHeight, stepFlag;
    private int nPixels, nAngles, radius, r2p1;

    private float PI = (float) Math.PI;
    private float angleInterval;

    public ImageStack[] calculate(ImageStack imsIn, int nAngles, int radius) {
        float[] pixels = ImageStackToFloatArray(imsIn);
        int w = imsIn.getWidth();
        int h = imsIn.getHeight();
        calculate(pixels, w, h, nAngles, radius);

        ImageStack[] imsArray = new ImageStack[3];
        imsArray[0] = ImageStackFromFloatArray(pixelsOut, w, h);
        imsArray[1] = ImageStackFromFloatArray(pixelsLFT, w, h);
        imsArray[2] = ImageStackFromFloatArray(pixelsLFTOrientation, w, h);
        return imsArray;
    }

    public float[] calculate(float[] pixels, int width, int height, int nAngles, int radius) {
        this.pixelsIn = pixels;
        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.nPixels = pixels.length;

        this.pixelsLFT = new float[nPixels];
        this.pixelsLFTOrientation = new float[nPixels];
        this.pixelsOut = new float[nPixels];

        this.nAngles = nAngles;
        this.angleInterval = PI/nAngles;
        this.radius = radius;
        this.r2p1 = radius * 2 + 1;

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();

        put(this.pixelsIn);
        put(this.pixelsOut);
        put(this.pixelsLFT);
        put(this.pixelsLFTOrientation);

        stepFlag = STEP_CALCULATE_LFT;
        executeByBlocks(this.nPixels);
        stepFlag = STEP_CALCULATE_OFT;
        executeByBlocks(this.nPixels);

        // Download arrays
        get(this.pixelsOut);
        get(this.pixelsLFT);
        get(this.pixelsLFTOrientation);

        return this.pixelsOut;
    }

    // called inside CL
    @Override
    public void run() {
        if (stepFlag == STEP_CALCULATE_LFT) calculateLFT();
        else if (stepFlag == STEP_CALCULATE_OFT) calculateOFT();
    }

    public void calculateLFT() {
        int p0 = getGlobalId() + blockOffset;
        int x0 = p0 % width;
        int y0 = (p0 / width) % height;
        int f0 = p0 / widthHeight;

        float maxAngleIntensity = 0;
        float maxIntensityAngle = 0;
        for (float k = 0; k < PI ; k = k + angleInterval) {
            float lineSum = 0;

            for (int q = -radius; q <= radius; q++) {
                int x = x0 + (int) floor(q * cos(k) + 0.5f); // axis may be inverted?
                int y = y0 - (int) floor(q * sin(k) + 0.5f);
                x = min(max(0, x), width - 1);
                y = min(max(0, y), height - 1);
                lineSum += pixelsIn[f0 * widthHeight + y * width + x];
            }

            if (maxAngleIntensity < lineSum) {
                maxAngleIntensity = lineSum;
                maxIntensityAngle = k;
            }
        }
        pixelsLFT[p0] = maxAngleIntensity / r2p1;
        pixelsLFTOrientation[p0] = maxIntensityAngle;
    }

    public void calculateOFT() {
        int p0 = getGlobalId() + blockOffset;
        int x0 = p0 % width;
        int y0 = (p0 / width) % height;
        int f0 = p0 / widthHeight;

        float maxAngleIntensity = 0;
        for (float k = 0; k < PI; k = k + angleInterval) {
            float lineSum = 0;

            for (int q = -radius; q <= radius; q++) {

                int x = x0 + (int) floor(q * cos(k) + 0.5f); // axis may be inverted?
                int y = y0 - (int) floor(q * sin(k) + 0.5f);
                x = min(max(0, x), width - 1);
                y = min(max(0, y), height - 1);

                float rho = pixelsLFT[p0];
                float theta = pixelsLFTOrientation[f0 * widthHeight + y * width + x];

                lineSum += rho * cos(2 * (theta - k));
            }
            maxAngleIntensity = max(lineSum, maxAngleIntensity);
        }
        pixelsOut[p0] = maxAngleIntensity / r2p1;
        pixelsLFTOrientation[p0] = pixelsLFTOrientation[p0] * (180/PI);
    }
}