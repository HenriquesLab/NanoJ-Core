package nanoj.core.java.image.analysis;

import nanoj.core.java.array.ArrayMath;
import nanoj.core.java.tools.Exceptions;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 3/7/13
 * Time: 5:10 PM
 */
public class CalculateNoiseAndBackground {
    public int xstart;
    public int ystart;
    public int regSize;
    public boolean throwZeroFound = true;
    public boolean throwSaturationFound = true;
    public int saturationLevel = 14000;

    private int width;
    private int height;
    private int widthHeight;


    public float[] calculateSigmaAndMeanInPeriphery(float[] pixels, int width, int height,
                                                    boolean averageSigmaAndMeanFrameValues){
        widthHeight = width*height;
        int nSlices = pixels.length / widthHeight;
        float mean = 0;
        float sum = 0;
        float stddev = 0;
        float v;
        int counter = (2 * width + 2 * height);
        int x, y, tp, yp;

        float [] sigmaAndMean;
        if (averageSigmaAndMeanFrameValues) {
            sigmaAndMean = new float[2];
            sigmaAndMean[0] = 0;
            sigmaAndMean[1] = 1;
        }
        else {
            sigmaAndMean = new float[nSlices*2];
        }

        for (int n=0; n<nSlices; n++) {
            sum = 0;
            mean = 0;
            stddev = 0;
            tp = widthHeight * n;

            // calculate mean
            y = 0; yp = y * width;
            for (x=0; x<width; x++)
                sum += pixels[tp + yp + x];
            y = height-1; yp = y * width;
            for (x=0; x<width; x++)
                sum += pixels[tp + yp + x];
            x = 0;
            for (y=0; y<height; y++)
                sum += pixels[tp + y * width + x];
            x = width-1;
            for (y=0; y<height; y++)
                sum += pixels[tp + y * width + x];
            mean = sum / counter;

            // calculate standard deviation
            sum = 0;
            y = 0; yp = y * width;
            for (x=0; x<width; x++)
                sum += Math.pow(pixels[tp + yp + x]-mean, 2);
            y = height-1; yp = y * width;
            for (x=0; x<width; x++)
                sum += Math.pow(pixels[tp + yp + x]-mean, 2);
            x = 0;
            for (y=0; y<height; y++)
                sum += Math.pow(pixels[tp + y * width + x]-mean, 2);
            x = width-1;
            for (y=0; y<height; y++)
                sum += Math.pow(pixels[tp + y * width + x]-mean, 2);
            stddev = (float) Math.sqrt(sum/counter);

            if (averageSigmaAndMeanFrameValues)
            {
                sigmaAndMean[0] += stddev / nSlices;
                sigmaAndMean[1] += mean / nSlices;
            }
            else {
                sigmaAndMean[n*2  ] = stddev;
                sigmaAndMean[n*2+1] = mean;
            }
        }
        return sigmaAndMean;
    }

    public float[] calculateSigmaAndMeanInCorners(float[] pixels, int width, int height,
                                                  boolean averageSigmaAndMeanFrameValues){

        widthHeight = width*height;
        int nSlices = pixels.length / widthHeight;
        float [] cornerSigmaAndMean;
        float [] sigmaAndMean;

        if (!averageSigmaAndMeanFrameValues) {
            sigmaAndMean = new float[nSlices*2];
        }
        else {
            sigmaAndMean = new float[2];
        }
        for (int n=0; n<sigmaAndMean.length; n++)
            sigmaAndMean[n] = 0;

        int count = 0;
        for (int position=-5; position<0; position++)
        {
            try
            {
                cornerSigmaAndMean = calculateSigmaAndMeanInRegion(pixels, width, height, 10,
                        position, averageSigmaAndMeanFrameValues);

                for (int n=0; n<cornerSigmaAndMean.length; n++)
                    sigmaAndMean[n] += cornerSigmaAndMean[n];
                count++;
            } catch (Exceptions.ZeroOrSaturationFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        if (count > 0)
        {
            for (int n=0; n<sigmaAndMean.length; n++)
                sigmaAndMean[n] /= count;
        }

        return sigmaAndMean;
    }

    /**
     * Calculates background noise sigma & mean
     * @param pixels image pixels, can be a stack of multiple images
     * @param width
     * @param height
     * @param regSize size of the region used to calculate sigma & mean
     * @param position position where the sigma & mean will be calculated:
     *                 -1 - top left,
     *                 -2 - top right,
     *                 -3 - bottom left,
     *                 -4 - bottom right,
     *                 -5 - center,
     *                 -6 - random region,
     *                 -7 - around minimum intensity pixel (slow as it goes through all pixels)
     *                 >=0 - pixel position where region starts
     * @param averageSigmaAndMeanFrameValues
     * @return background sigma and mean, or values of sigma and mean for each frame in the stack
     */
    public float[] calculateSigmaAndMeanInRegion(float[] pixels, int width, int height, int regSize,
                                                 int position, boolean averageSigmaAndMeanFrameValues)
            throws Exceptions.ZeroOrSaturationFoundException {
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.regSize = regSize;

        int nSlices = pixels.length / widthHeight;

        if (position == -1) // top left
        {
            xstart = 0;
            ystart = 0;
        }
        else if (position == -2) // top right
        {
            xstart = width-regSize-1;
            ystart = 0;
        }
        else if (position == -3) // bottom left
        {
            xstart = 0;
            ystart = height-regSize-1;
        }
        else if (position == -4) // bottom right
        {
            xstart = width-regSize-1;
            ystart = height-regSize-1;
        }
        else if (position == -5) // center
        {
            xstart = width/2-regSize/2-1;
            ystart = height/2-regSize/2-1;
        }
        else if (position == -6) // random position
        {
            xstart = (int)(Math.random()*(width-regSize));
            ystart = (int)(Math.random()*(height-regSize));
        }
        else if (position == -7) // around minimum
        {
            int p = (int) ArrayMath.getMinValue(pixels)[0];
            int x = p % (width);
            int y = (p / width) % height;
            xstart = (x-regSize/2-1);
            ystart = (y-regSize/2-1);
        }
        else
        {
            int p = position;
            int x = p % (width);
            int y = (p / width) % height;
            xstart = (x-regSize/2-1);
            ystart = (y-regSize/2-1);

        }
        xstart = Math.min(xstart, width-regSize);
        xstart = Math.max(xstart, 0);
        ystart = Math.min(ystart, height-regSize);
        ystart = Math.max(ystart, 0);

        float [] sliceSigmaAndMean;
        float [] sigmaAndMean;

        if (averageSigmaAndMeanFrameValues) {
            sigmaAndMean = new float [2];
            sigmaAndMean[0] = 0;
            sigmaAndMean[1] = 0;
            for (int n=0;n<nSlices;n++)
            {
                sliceSigmaAndMean = getSigmaAndMeanInSquare(pixels, n);
                sigmaAndMean[0] += sliceSigmaAndMean[0];
                sigmaAndMean[1] += sliceSigmaAndMean[1];
            }
            sigmaAndMean[0] /= nSlices;
            sigmaAndMean[1] /= nSlices;
        }

        else {
            sigmaAndMean = new float [2*nSlices];
            for (int n=0;n<nSlices;n++)
            {
                sliceSigmaAndMean = getSigmaAndMeanInSquare(pixels, n);
                sigmaAndMean[2*n]   = sliceSigmaAndMean[0];
                sigmaAndMean[2*n+1] = sliceSigmaAndMean[1];
            }
        }
        return sigmaAndMean;
    }

    private float[] getSigmaAndMeanInSquare(float[] pixels, int slice)
            throws Exceptions.ZeroOrSaturationFoundException {

        float mean = 0;
        float sum = 0;
        int regSize2 = regSize*regSize;
        float v;

        for (int i=0; i<=regSize; i++){
            for (int j=0; j<=regSize; j++){
                v = pixels[slice * widthHeight + j * width + i];
                if (v == 0 && throwZeroFound)
                    throw new Exceptions.ZeroOrSaturationFoundException(
                            "Found zero in region where calculating sigma and mean on frame "+slice);
                if (v >= saturationLevel && throwSaturationFound)
                    throw new Exceptions.ZeroOrSaturationFoundException(
                            "Found saturation in region where calculating sigma and mean on frame "+slice);
                sum += v;
            }
        }
        mean = sum / regSize2;
        sum = 0; // not really sum anymore

        for (int i=xstart; i<=xstart+regSize; i++){
            for (int j=ystart; j<=ystart+regSize; j++){
                v = pixels[slice * widthHeight + j*width + i];
                sum += Math.pow(v-mean, 2);
            }
        }
        float stddev = (float) Math.sqrt(sum/regSize2);

        float [] sigmaMean = new float [2];
        sigmaMean[0] = stddev;
        sigmaMean[1] = mean;
        return sigmaMean;
    }

}
