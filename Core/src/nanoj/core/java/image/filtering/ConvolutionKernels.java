package nanoj.core.java.image.filtering;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.array.ImageStackToFromArray;

import static java.lang.Math.*;
import static org.apache.commons.math3.special.Erf.erf;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 3/21/13
 * Time: 12:32 PM
 */
public class ConvolutionKernels {
    private final static double ROOT2 = sqrt(2);

    ///////////////////////////////////
    // 2D Kernels - for 3D see below //
    ///////////////////////////////////

    /**
     * Creates a normalized gaussian kernel to be used in convolutions
     * @param sigma gaussian sigma
     * @return normalized gaussian kernel
     */
    public static FloatProcessor genGaussianKernel(double sigma){
        int size = (int) Math.max(Math.round(sigma * 5)+1, 3);
        if (size % 2 == 0) size += 1; // make sure we have a 2n+1 matrix
        float [] kernel = new float[size*size];
        float sum = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = y * size + x;
                double xx = x - size / 2;
                double yy = y - size / 2;
                kernel[p] = (float) pow(Math.E, -(xx * xx + yy * yy)
                        / (2 * (sigma * sigma)));
                sum += kernel[p];
            }
        }
        if (sum!=0) for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        return new FloatProcessor(size, size, kernel);
    }

    public static FloatProcessor genGaussianERFKernel(double sigma) {
        return genGaussianERFKernel(sigma, 0, 0);
    }

    public static FloatProcessor genGaussianERFKernel(double sigma, double xShift, double yShift){
        // https://stackoverflow.com/questions/16165666/how-to-determine-the-window-size-of-a-gaussian-filter
        int size = Math.max((int)(sigma * 3)+1, 3);
        if (size % 2 == 0) size += 1; // make sure we have a 2n+1 matrix
        float [] kernel = new float[size*size];
        float sum = 0;

        double sigma2 = ROOT2*sigma; // 2 * pow(sigma, 2);

        // based on Smith et al, Nath Meth, 2010: Fast, single-molecule localization that achieves theoretically
        // minimum uncertainty (see Sup Mat page 10)
        // note, the paper has an error on their formula 4a and 4b, 2sigma^2 should be sqrt(2)*sigma
        // see https://en.wikipedia.org/wiki/Normal_distribution formula 'Cumulative distribution function'
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                int p = y * size + x;
                double dx = x - size / 2 + xShift;
                double dy = y - size / 2 + yShift;

                double Ex = 0.5f * (erf((dx + 0.5f) / sigma2) - erf((dx - 0.5f) / sigma2));
                double Ey = 0.5f * (erf((dy + 0.5f) / sigma2) - erf((dy - 0.5f) / sigma2));
                float v = (float) (Ex * Ey);

                kernel[p] = v;
                sum += v;
            }
        }
        if (sum!=0) for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        return new FloatProcessor(size, size, kernel);
    }

    /**
     * Creates a normalized off-center gaussian kernel to be used in convolutions
     * @param sigma gaussian sigma
     * @param xDelta deviation in the x-axis (e.g -0.5 takes it half-pixel to the left)
     * @param yDelta deviation in the y-axis
     * @return normalized gaussian kernel
     */
    public static FloatProcessor genGaussianKernel(double sigma, float xDelta, float yDelta){
        int size = (int) Math.max(Math.round(sigma * 5)+1, 3);
        if (size % 2 == 0) size += 1; // make sure we have a 2n+1 matrix
        float [] kernel = new float[size*size];
        float sum = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = y * size + x;
                double xx = x - (size / 2)+xDelta;
                double yy = y - (size / 2)+yDelta;
                kernel[p] = (float) pow(Math.E, -(xx * xx + yy * yy)
                        / (2 * (sigma * sigma)));
                sum += kernel[p];
            }
        }

        for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        return new FloatProcessor(size, size, kernel);
    }


    /**
     * Creates a normalized LoG kernel to be used in convolutions
     * @param sigma gaussian sigma
     * @return normalized LoG kernel
     */
    public static FloatProcessor genLaplacianOfGaussianKernel(double sigma) {
        int size = (int) Math.max(Math.round(sigma * 5)+1, 3);
        if (size % 2 == 0) size += 1; // make sure we have a 2n+1 matrix
        float [] kernel = new float[size*size];
        float sum = 0;

        final double a = -1/(Math.PI* pow(sigma, 4));
        final double b = (2*sigma*sigma);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = y * size + x;
                double xx = x - size / 2;
                double yy = y - size / 2;

                double c = -(xx * xx + yy * yy)/b;

                kernel[p] = (float) (a*(1+c)* pow(Math.E, c));
                sum += kernel[p];
            }
        }

        for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        return new FloatProcessor(size, size, kernel);
    }


    /**
     * Creates a DoG kernel to be used in convolutions
     * @param sigmaSmall sigma of small gaussian
     * @param sigmaBig sigma of big gaussian
     * @return DoG kernel
     */
    public static FloatProcessor genDifferenceOfGaussiansKernel(double sigmaSmall, double sigmaBig){
        int size = (int) Math.max(Math.round(sigmaBig * 5)+1, 3);
        if (size % 2 == 0) size += 1; // make sure we have a 2n+1 matrix
        float [] kernelSmall = new float[size*size];
        float [] kernelBig   = new float[size*size];

        // create small sigma kernel
        float sum = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = y * size + x;
                double xx = x - size / 2;
                double yy = y - size / 2;
                kernelSmall[p] = (float) pow(Math.E, -(xx * xx + yy * yy)
                        / (2 * (sigmaSmall * sigmaSmall)));
                sum += kernelSmall[p];
            }
        }
        for (int i = 0; i < kernelSmall.length; i++) kernelSmall[i] /= sum;

        // create big sigma kernel
        sum = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = y * size + x;
                double xx = x - size / 2;
                double yy = y - size / 2;
                kernelBig[p] = (float) pow(Math.E, -(xx * xx + yy * yy)
                        / (2 * (sigmaBig * sigmaBig)));
                sum += kernelBig[p];
            }
        }
        for (int i = 0; i < kernelBig.length; i++) kernelBig[i] /= sum;

        // subtract the two
        for (int i = 0; i < kernelSmall.length; i++) kernelSmall[i] -= kernelBig[i];

        return new FloatProcessor(size, size, kernelSmall);
    }

    public static FloatProcessor genSmooth(int xSize, int ySize) {
        float [] kernel = new float[xSize*ySize];
        for (int i = 0; i < kernel.length; i++) kernel[i] = 1f/kernel.length;
        return new FloatProcessor(xSize, ySize, kernel);
    }

    public static FloatProcessor genIntegration(int xSize, int ySize) {
        float [] kernel = new float[xSize*ySize];
        for (int i = 0; i < kernel.length; i++) kernel[i] = 1f;
        return new FloatProcessor(xSize, ySize, kernel);
    }

    public static FloatProcessor genCircularIntegration(double radius) {
        int w = (int) (ceil(radius)*2+1);
        int h = (int) (ceil(radius)*2+1);
        float [] kernel = new float[w*h];
        for (int j=0; j<h; j++) {
            double yR = j - radius;
            for (int i=0; i<w; i++) {
                double xR = i - radius;
                if (sqrt(xR*xR+yR*yR) <= radius) {
                    kernel[w*j+i] = 1;
                }
            }
        }
        return new FloatProcessor(w, h, kernel);
    }

    public static FloatProcessor genCircularSmooth(double radius) {
        FloatProcessor fpKernel = genCircularIntegration(radius);
        float[] kernel = (float[]) fpKernel.getPixels();
        normalizeToOne(kernel);
        return fpKernel;
    }

    public static FloatProcessor genLaplacian1() {
        float [] kernel = new float[]{
                 0,-1, 0,
                -1, 4, -1,
                 0, -1, 0};
        return new FloatProcessor(3, 3, kernel);
    }

    public static FloatProcessor genLaplacian2() {
        float [] kernel = new float[]{
                -1,-1,-1,
                -1, 8,-1,
                -1,-1,-1};
        return new FloatProcessor(3, 3, kernel);
    }
    public static FloatProcessor genLaplacian3() {
        float [] kernel = new float[]{
                -0.5f,-1,-0.5f,
                -1, 6,-1,
                -0.5f,-1,-0.5f};
        return new FloatProcessor(3, 3, kernel);
    }

    public static FloatProcessor genExtendedLaplacian() {
        float [] kernel = new float[]{
                -1,-1,-1,-1,-1,
                -1,-1,-1,-1,-1,
                -1,-1,24,-1,-1,
                -1,-1,-1,-1,-1,
                -1,-1,-1,-1,-1};
        return new FloatProcessor(5, 5, kernel);
    }
    public static FloatProcessor genExtendedLaplacian2() {
        float [] kernel = new float[]{
                -10,-5,-2,-1,-2,-5,-10,
                -5 , 0, 3, 4, 3, 0, -5,
                -2 , 3, 6, 7, 6, 3, -2,
                -1 , 4, 7, 8, 7, 4, -1,
                -2 , 3, 6, 7, 6, 3, -2,
                -5 , 0, 3, 4, 3, 0, -5,
                -10,-5,-2,-1,-2,-5,-10};
        return new FloatProcessor(7, 7, kernel);
    }

    public static FloatProcessor genGradientX(int xSize, int ySize) {
        assert xSize>0;
        assert ySize>=0;
        int width = (2*xSize+1);
        int height = (2*ySize+1);

        float [] kernel = new float[width*height];

        for (int j=-ySize;j<=ySize;j++){
            for (int i=-xSize;i<=xSize;i++){
                if (i<0) {
                    kernel[(ySize+j)*width+(xSize+i)] = -1;
                }
                if (i>0) {
                    kernel[(ySize+j)*width+(xSize+i)] = 1;
                }
            }
        }
        return new FloatProcessor(width, height, kernel);
    }

    public static FloatProcessor genGradientY(int xSize, int ySize) {
        assert xSize>=0;
        assert ySize>0;
        int width = (2*xSize+1);
        int height = (2*ySize+1);

        float [] kernel = new float[width*height];

        for (int j=-ySize;j<=ySize;j++){
            for (int i=-xSize;i<=xSize;i++){
                if (j<0) {
                    kernel[(ySize+j)*width+(xSize+i)] = -1;
                }
                if (j>0) {
                    kernel[(ySize+j)*width+(xSize+i)] = 1;
                }
            }
        }
        return new FloatProcessor(width, height, kernel);
    }

    ////////////////
    // 3D Kernels //
    ////////////////
    public static ImageStack gen3DTemporalFrameSubtraction(int xSize, int ySize, int innerTRadius, int outerTDelta) {
        // TODO: RH thinks there is something wrong with this still... will have to be fixed
        assert xSize>0;
        assert ySize>0;
        int width = (2*xSize+1);
        int height = (2*ySize+1);
        int widthHeight = width*height;
        int tSize = 2*innerTRadius+1+outerTDelta*2;
        int innerTDelta = 2*innerTRadius+1;

        float [][] kernel = new float[tSize][widthHeight];
        int totalPixels = xSize*ySize*tSize;
        int counter = 0;

        for (int t=0;t<tSize;t++) { // note that t is not centered as i and j
            for (int j = -ySize; j <= ySize; j++) {
                for (int i = -xSize; i <= xSize; i++) {
                    if (t<outerTDelta) {
                        kernel[t][(ySize+j)*width+(xSize+i)] = -1f;
                        counter++;
                    }
                    else if (t>=tSize-outerTDelta) {
                        kernel[t][(ySize + j) * width + (xSize + i)] = -1f;
                        counter++;
                    }
                }
            }
        }
        kernel[tSize/2][ySize*width+xSize] = counter + 1;
        ImageStack ims = new ImageStack(width, height);
        for (int t=0;t<tSize;t++){
            ims.addSlice(new FloatProcessor(width, height, kernel[t]));
        }
        return ims;
    }

    public static ImageStack gen3DSmooth() {

        float [][] kernel = new float[3][];
        for (int t=0;t<3;t++) { // note that t is not centered as i and j
            kernel[t] = new float[] {1/27f,1/27f,1/27f,1/27f,1/27f,1/27f,1/27f,1/27f,1/27f};
        }

        ImageStack ims = new ImageStack(3, 3);
        for (int t=0;t<3;t++){
            ims.addSlice(new FloatProcessor(3, 3, kernel[t]));
        }
        return ims;
    }

    public static ImageStack gen3DSmooth(int xSize, int ySize, int tSize) {
        float [] kernel = new float[tSize*xSize*ySize];
        for (int i = 0; i < kernel.length; i++) kernel[i] = 1f/kernel.length;
        return ImageStackToFromArray.ImageStackFromFloatArray(kernel, xSize, ySize);
    }

    //////////////////////////
    // Kernel normalization //
    //////////////////////////
    public static void normalizeToOne(float [] pixels) {
        float sum = 0;
        for (int i = 0; i < pixels.length; i++) sum += pixels[i];
        assert (sum!=0);
        for (int i = 0; i < pixels.length; i++) pixels[i] /= sum;
    }

    public static void normalizeToOne(ImageStack ims) {
        float [] kernel = ImageStackToFromArray.ImageStackToFloatArray(ims);
        normalizeToOne(kernel);
    }

    public static void normalizeToOne(FloatProcessor fp) {
        float [] kernel = (float []) fp.getPixels();
        normalizeToOne(kernel);
    }

    public static boolean isNormalizedToOne(float [] pixels, double allowedError) {
        float value = 0;
        for (int i = 0; i < pixels.length; i++) value += pixels[i];
        float error = Math.abs(value-1);
        if (error>allowedError) return false;
        return true;
    }

    public static boolean isNormalizedToOne(ImageStack ims, double allowedError) {
        float [] kernel = ImageStackToFromArray.ImageStackToFloatArray(ims);
        return isNormalizedToOne(kernel, allowedError);
    }

    public static boolean isNormalizedToOne(FloatProcessor fp, double allowedError) {
        float [] kernel = (float []) fp.getPixels();
        return isNormalizedToOne(kernel, allowedError);
    }

    public static boolean isNormalizedToZero(float [] pixels, double allowedError) {
        float value = 0;
        for (int i = 0; i < pixels.length; i++) value += pixels[i];
        if (Math.abs(value)>allowedError) return false;
        return true;
    }

    public static boolean isNormalizedToZero(ImageStack ims, double allowedError) {
        float [] kernel = ImageStackToFromArray.ImageStackToFloatArray(ims);
        return isNormalizedToZero(kernel, allowedError);
    }

    public static boolean isNormalizedToZero(FloatProcessor fp, double allowedError) {
        float [] kernel = (float []) fp.getPixels();
        return isNormalizedToZero(kernel, allowedError);
    }
}