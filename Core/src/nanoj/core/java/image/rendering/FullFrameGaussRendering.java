package nanoj.core.java.image.rendering;

import ij.IJ;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;

/**
 * Created by Nils Gustafsson on 28/07/15.
 */
public class FullFrameGaussRendering {
    static Kernel_FullFrameGaussRendering myAddParticles = new Kernel_FullFrameGaussRendering();

    public void addGaussians(FloatProcessor ip, float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels){
        float[] pixels;
        pixels = addGaussians((float []) ip.getPixels(), ip.getWidth(), ip.getHeight(), intensity, sigmaX, sigmaY, x, y, subPixels);
        ip.setPixels(pixels);
    }

    public void addGaussians(FloatProcessor ip, float[] intensity, float[] sigma, float[] x, float[] y){
        float[] pixels;
        pixels = addGaussians((float []) ip.getPixels(), ip.getWidth(), ip.getHeight(), intensity, sigma, sigma, x, y,1);
        ip.setPixels(pixels);
    }

    public void addGaussians(FloatProcessor ip, float[] intensity,
                             float[] sigmaX, float[] sigmaY, float[] x, float[] y){
        float[] pixels;
        pixels = addGaussians((float []) ip.getPixels(), ip.getWidth(), ip.getHeight(), intensity,
                sigmaX, sigmaY, x, y,1);
        ip.setPixels(pixels);
    }

    public void addGaussians(FloatProcessor ip, float intensity,
                             float sigmaX, float sigmaY, float [] x, float [] y){
        float [] intensity_ = new float[x.length];
        float [] sigmaX_ = new float[x.length];
        float [] sigmaY_ = new float[x.length];

        for (int n=0;n<x.length;n++) {
            intensity_[n] = intensity;
            sigmaX_[n] = sigmaX;
            sigmaY_[n] = sigmaY;
        }
        addGaussians(ip, intensity_, sigmaX_, sigmaY_, x, y);
    }

    public void addGaussians(FloatProcessor ip, float intensity, float sigma, float [] x, float [] y){
        float [] intensity_ = new float[x.length];
        float [] sigma_ = new float[x.length];

        for (int n=0;n<x.length;n++) {
            intensity_[n] = intensity;
            sigma_[n] = sigma;
        }
        addGaussians(ip, intensity_, sigma_, x, y);
    }

    public float[] addGaussians(float[] pixels, int width, int height,
                                float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels) {

        return myAddParticles.drawParticles(pixels, width, height, intensity, sigmaX, sigmaY, x, y, subPixels);
    }
}
class Kernel_FullFrameGaussRendering extends NJKernel{

    private float [] pixels;
    private float [] intensity;
    private float [] sigmaX;
    private float [] sigmaY;
    private float [] x;
    private float [] y;
    private int width, height, subPixels, nParts;
    private float centerSubPixel;
    private float subPixelSize;
    private int p;

    public float[] drawParticles(float[] pixels, int width, int height,
                                 float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels){
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.subPixels = subPixels;
        this.intensity = intensity;
        this.sigmaX = sigmaX;
        this.sigmaY = sigmaY;
        this.x = x;
        this.y = y;
        centerSubPixel = 0.5f / subPixels;
        subPixelSize = 1f / subPixels;
        nParts = intensity.length;

        setExplicit(true);
        autoChooseDeviceForNanoJ();

        put(this.pixels);
        put(this.intensity);
        put(this.sigmaX);
        put(this.sigmaY);
        put(this.x);
        put(this.y);

        for (p = 0; p < this.intensity.length; p++) {
            execute(this.pixels.length);
            IJ.showProgress(p, this.intensity.length - 1);
        }

        get(this.pixels);

        return this.pixels;
    }

    @Override
    public void run(){
        int pixelIdx = getGlobalId();
        int xPix = pixelIdx % width;
        int yPix = (pixelIdx / width) % height;

        float xSubPix = 0;
        float ySubPix = 0;

        float subIntensity = 0;
        float sigmaX2 = 0;
        float sigmaY2 = 0;
        float v = 0;

        //for(int p = 0; p < nParts; p++){
            subIntensity = intensity[p] / subPixels / subPixels;
            sigmaX2 = 2f * pow(sigmaX[p],2f);
            sigmaY2 = 2f * pow(sigmaY[p],2f);

            for(int i = 0; i < subPixels; i++){
                for(int j = 0; j < subPixels; j++){

                    xSubPix = (float) xPix + subPixelSize * i + centerSubPixel;
                    ySubPix = (float) yPix + subPixelSize * j + centerSubPixel;

                    v = v + subIntensity * exp(-pow((xSubPix - x[p]), 2f) / sigmaX2
                            - pow((ySubPix - y[p]), 2f) / sigmaY2);

                }
            }

        //}

        pixels[pixelIdx] = pixels[pixelIdx] + v;

    }
}