package nanoj.core.java.image.filtering;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ImageStackToFromArray;


/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 3/28/13
 * Time: 6:13 PM
 */

public class Convolve {

    private static Kernel_convolve2DStack kernel2d = new Kernel_convolve2DStack();
    private static Kernel_convolve3DStack kernel3d = new Kernel_convolve3DStack();

    public String getExecutionMode(){
        return kernel2d.getExecutionMode().toString();
    }

    public synchronized  FloatProcessor convolve2D(FloatProcessor ip, FloatProcessor convMatrix) {
        float [] pixels = (float []) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        float [] cm = (float[]) convMatrix.getPixels();
        int cm_width = convMatrix.getWidth();
        int cm_height = convMatrix.getHeight();
        pixels = convolve2DStack(pixels, width, height, cm, cm_width, cm_height);
        return new FloatProcessor(width, height, pixels);
    }

    public void convolve2DStack(ImagePlus imp, FloatProcessor convMatrix) {
        ImageStack ims = imp.getStack();
        ims = convolve2DStack(ims, convMatrix);
        imp.setStack(ims);
    }

    public void convolve3DStack(ImagePlus imp, ImageStack convMatrix) {
        ImageStack ims = imp.getStack();
        ims = convolve3DStack(ims, convMatrix);
        imp.setStack(ims);
    }

    public ImageStack convolve2DStack(ImageStack ims, FloatProcessor convMatrix) {
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);
        int width = ims.getWidth();
        int height = ims.getHeight();
        float [] cm = (float[]) convMatrix.getPixels();
        int cm_width = convMatrix.getWidth();
        int cm_height = convMatrix.getHeight();

        pixels = convolve2DStack(pixels, width, height, cm, cm_width, cm_height);
        return ImageStackToFromArray.ImageStackFromFloatArray(pixels, width, height);
    }

    public ImageStack convolve3DStack(ImageStack ims, ImageStack convMatrix) {
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);
        int width = ims.getWidth();
        int height = ims.getHeight();
        float [] cm = ImageStackToFromArray.ImageStackToFloatArray(convMatrix);
        int cm_width = convMatrix.getWidth();
        int cm_height = convMatrix.getHeight();

        pixels = convolve3DStack(pixels, width, height, cm, cm_width, cm_height);
        return ImageStackToFromArray.ImageStackFromFloatArray(pixels, width, height);
    }

    public synchronized float[] convolve2DStack(float[] pixels, int width, int height,
                                        FloatProcessor convMatrix) {
        float [] cm = (float[]) convMatrix.getPixels();
        int cm_width = convMatrix.getWidth();
        int cm_height = convMatrix.getHeight();
        pixels = kernel2d.convolve(pixels, width, height, cm, cm_width, cm_height);
        return pixels;
    }

    public float[] convolve3DStack(float[] pixels, int width, int height,
                                   ImageStack convMatrix) {
        float [] cm = ImageStackToFromArray.ImageStackToFloatArray(convMatrix);
        int cm_width = convMatrix.getWidth();
        int cm_height = convMatrix.getHeight();
        pixels = kernel3d.convolve(pixels, width, height, cm, cm_width, cm_height);
        return pixels;
    }

    public float[] convolve2DStack(float[] pixels, int width, int height,
                                   float[] convMatrix, int cm_width, int cm_height) {
        pixels = kernel2d.convolve(pixels, width, height, convMatrix, cm_width, cm_height);
        return pixels;
    }

    public float[] convolve3DStack(float[] pixels, int width, int height,
                                   float[] convMatrix, int cm_width, int cm_height) {
        pixels = kernel3d.convolve(pixels, width, height, convMatrix, cm_width, cm_height);
        return pixels;
    }
}

class Kernel_convolve2DStack extends NJKernel {

    private float convMatrix_$constant$[];
    private int width, height, widthHeight;
    private int conv_width, conv_height, offset_width, offset_height;
    private float imageIn[];
    private float imageOut[];

    public float [] convolve(float [] pixels, int width, int height, float [] convMatrix, int cm_width, int cm_height){

        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.conv_width = cm_width;
        this.conv_height = cm_height;
        this.convMatrix_$constant$ = convMatrix;
        this.imageIn = pixels;
        this.imageOut = new float [imageIn.length];

        if (conv_width%2!=0) offset_width = (conv_width-1)/2;
        else offset_width = conv_width / 2;
        if (conv_height%2!=0) offset_height = (conv_height-1)/2;
        else offset_height = offset_height / 2;

        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(imageIn);
        put(convMatrix_$constant$);
        put(imageOut);

        executeByBlocks(pixels.length);

        get(imageOut);
        return imageOut;
    }

    @Override public void run() {
        int p, x, y, z;
        int _x, _y, _p;

        p = getGlobalId(0) + blockOffset;
        x = p % (width);
        y = (p / width) % height;
        z = p / (widthHeight);

        int _pz = z * widthHeight;
        float accum = 0;

        for (int cx = 0; cx < conv_width; cx++){
            _x = min(max(x + cx - offset_width, 0), width -1);

            for (int cy = 0; cy < conv_height; cy++){
                _y = min(max(y + cy - offset_height, 0), height -1);

                _p = _pz + _y * width + _x;
                accum += imageIn[_p] * convMatrix_$constant$[ cy * conv_width + cx ];
            }
        }
        imageOut[_pz + y * width + x] = accum;
    }
}

class Kernel_convolve3DStack extends NJKernel {

    private float convMatrix_$constant$[];
    private int width, height, widthHeight, depth;
    private int conv_width, conv_height, conv_depth, conv_widthHeight, offset_width, offset_height, offset_depth;
    private float imageIn[];
    private float imageOut[];

    public float [] convolve(float [] pixels, int width, int height, float [] convMatrix, int cm_width, int cm_height){

        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.depth = pixels.length/widthHeight;
        this.conv_width = cm_width;
        this.conv_height = cm_height;
        this.conv_widthHeight = cm_width*cm_height;
        this.conv_depth = convMatrix.length/conv_widthHeight;
        this.convMatrix_$constant$ = convMatrix;
        this.imageIn = pixels;
        this.imageOut = new float [imageIn.length];
        if (conv_width%2!=0) offset_width = (conv_width-1)/2;
        else offset_width = conv_width / 2;
        if (conv_height%2!=0) offset_height = (conv_height-1)/2;
        else offset_height = offset_height / 2;
        if (conv_depth%2!=0) offset_depth = (conv_depth-1)/2;
        else offset_depth = conv_depth / 2;

        int depth = pixels.length / this.widthHeight;
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(imageIn);
        put(this.convMatrix_$constant$);
        put(imageOut);
        execute(pixels.length);
        get(imageOut);
        return imageOut;
    }

    @Override public void run() {
        int p, x, y, t;
        int _x, _y, _p, _t,_pt;

        p = getGlobalId(0);
        x = p % (width);
        y = (p / width) % height;
        t = p / (widthHeight);


        float accum = 0;

        for (int ct=0; ct < conv_depth; ct++) {
            _t = min(max(t + ct - offset_depth, 0), depth -1);
            _pt = _t * widthHeight;

            for (int cx = 0; cx < conv_width; cx++){
                _x = min(max(x + cx - offset_width, 0), width -1);

                for (int cy = 0; cy < conv_height; cy++){
                    _y = min(max(y + cy - offset_height, 0), height -1);

                    _p = _pt + _y * width + _x;
                    accum += imageIn[_p] * convMatrix_$constant$[ ct*conv_depth +cy * conv_width + cx ];
                }
            }
        }

        imageOut[t*widthHeight + y * width + x] = accum;
    }
}
