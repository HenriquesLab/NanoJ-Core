package nanoj.core.java.image.rendering;

import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ArrayTypeConversion;
import nanoj.core.java.tools.Log;

/**
 * Author: Nils Gustafsson
 * Date: 12/02/15
 */
public class SubPixelGaussianRendering {

    static Kernel_subPixelGaussianRendering mySubPixelAddParticles = new Kernel_subPixelGaussianRendering();
    static Kernel_ERFGaussianRendering myERFAddParticles = new Kernel_ERFGaussianRendering();

    public void addGaussians(FloatProcessor ip, float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels){
        float[] pixels;
        pixels = addGaussians((float []) ip.getPixels(), ip.getWidth(), ip.getHeight(), intensity, sigmaX, sigmaY, x, y, subPixels);
        ip.setPixels(pixels);
    }

    public void addGaussians(FloatProcessor ip, float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y){
        float[] pixels;
        pixels = addGaussians((float []) ip.getPixels(), ip.getWidth(), ip.getHeight(), intensity, sigmaX, sigmaY, x, y);
        ip.setPixels(pixels);
    }

    public float[] addGaussians(float[] pixels, int width, int height,
                                float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels) {

        return mySubPixelAddParticles.drawParticles(pixels, width, height, intensity, sigmaX, sigmaY, x, y, subPixels);
    }

    public float[] addGaussians(float[] pixels, int width, int height,
                                float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y) {

        return myERFAddParticles.drawParticles(pixels, width, height, intensity, sigmaX, sigmaY, x, y);
    }
}

class Kernel_subPixelGaussianRendering extends NJKernel {
    private float [] pixels;
    private int [] pixels_encodedFloatToInt;
    private float [] intensity_$constant$;
    private float [] sigmaX_$constant$;
    private float [] sigmaY_$constant$;
    private float [] x_$constant$;
    private float [] y_$constant$;
    private int width, height, subPixels;
    public int precision = 3; // decimal places
    private int precisionMultiplier = (int) pow(10, precision);

    public float[] drawParticles(float[] pixels, int width, int height,
                              float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y, int subPixels){
        this.pixels = pixels;
        this.pixels_encodedFloatToInt = ArrayTypeConversion.encodeFloatArrayIntoInt(pixels, precision);
        this.width = width;
        this.height = height;
        this.subPixels = subPixels;
        this.intensity_$constant$ = intensity;
        this.sigmaX_$constant$ = sigmaX;
        this.sigmaY_$constant$ = sigmaY;
        this.x_$constant$ = x;
        this.y_$constant$ = y;

        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixels_encodedFloatToInt);
        put(this.intensity_$constant$);
        put(this.sigmaX_$constant$);
        put(this.sigmaY_$constant$);
        put(this.x_$constant$);
        put(this.y_$constant$);
        execute(this.intensity_$constant$.length);
        get(this.pixels_encodedFloatToInt);

        this.pixels = ArrayTypeConversion.decodeFloatArrayFromInt(pixels_encodedFloatToInt, precision);
        return this.pixels;
    }

    @Override
    public void run() {
        int p = getGlobalId(0);
        float x = x_$constant$[p] * subPixels;
        float y = y_$constant$[p] * subPixels;
        int rx = round(x);
        int ry = round(y);
        float intensity = intensity_$constant$[p] / subPixels / subPixels;
        float sigmaX = sigmaX_$constant$[p] * subPixels;
        float sigmaY = sigmaY_$constant$[p] * subPixels;
        float sigmaX2 = 2*pow(sigmaX, 2);
        float sigmaY2 = 2*pow(sigmaY, 2);
        float v;

        int radiusX = (int) (sigmaX * 2.354f) + 3;
        int radiusY = (int) (sigmaY * 2.354f) + 3;

        int i_;
        int j_;

        for (int i = rx-radiusX; i<=rx+radiusX; i++) {
            i_ = i / subPixels;
            if (i_ < 0 || i_ > width - 1) continue;

            for (int j = ry-radiusY; j<=ry+radiusY; j++) {
                j_ = j / subPixels;
                if (j_ < 0 || j_ > height - 1) continue;

                v = intensity * exp(-pow((i+0.5f-x),2)/sigmaX2-pow((j+0.5f-y), 2)/sigmaY2);
                atomicAdd(pixels_encodedFloatToInt, j_ * width + i_, round(v* precisionMultiplier));
            }
        }
    }
}

class Kernel_ERFGaussianRendering extends NJKernel {
    private int MAX_PARTICLES_PER_RUN = 1000;
    private float ROOT2 = sqrt(2);

    private float [] pixels;
    private float [] intensity_$constant$;
    private float [] sigmaX_$constant$;
    private float [] sigmaY_$constant$;
    private float [] x_$constant$;
    private float [] y_$constant$;
    private int width, height;
    private int nStart, nEnd;
    private Log log = new Log();

    public float[] drawParticles(float[] pixels, int width, int height,
                                 float[] intensity, float[] sigmaX, float[] sigmaY, float[] x, float[] y){

        if (intensity.length == 0) return pixels;
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.intensity_$constant$ = intensity;
        this.sigmaX_$constant$ = sigmaX;
        this.sigmaY_$constant$ = sigmaY;
        this.x_$constant$ = x;
        this.y_$constant$ = y;

        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.intensity_$constant$);
        put(this.sigmaX_$constant$);
        put(this.sigmaY_$constant$);
        put(this.x_$constant$);
        put(this.y_$constant$);

        int nParticles = intensity.length;
        int groups = nParticles / MAX_PARTICLES_PER_RUN;
        if (nParticles % MAX_PARTICLES_PER_RUN != 0) groups++;

        for (int pG = 0; pG<groups; pG++) {
            nStart = pG * MAX_PARTICLES_PER_RUN;
            nEnd = min((pG + 1) * MAX_PARTICLES_PER_RUN, nParticles);
            execute(pixels.length);
        }

        get(this.pixels);

        return this.pixels;
    }

    @Override
    public void run() {
        int p = getGlobalId(0);

        int x = p % width;
        int y = p / width;

        float v = pixels[p];
        for (int pId=nStart; pId<nEnd; pId++) {
            float xp = x_$constant$[pId];
            float yp = y_$constant$[pId];
            float intensity = intensity_$constant$[pId];
            float sigmaX = sigmaX_$constant$[pId];
            float sigmaY = sigmaY_$constant$[pId];
            float dx = x - xp;
            float dy = y - yp;

            if (abs(dx)<sigmaX*10 || abs(dy)<sigmaY*10) {
                float sigmaX2 = ROOT2*sigmaX; // 2 * pow(sigmaX, 2);
                float sigmaY2 = ROOT2*sigmaY; //2 * pow(sigmaY, 2);

                // based on Smith et al, Nath Meth, 2010: Fast, single-molecule localization that achieves theoretically
                // minimum uncertainty (see Sup Mat page 10)
                // note, the paper has an error on their formula 4a and 4b, 2sigma^2 should be sqrt(2)*sigma
                // see https://en.wikipedia.org/wiki/Normal_distribution formula 'Cumulative distribution function'
                float Ex = 0.5f * (erf((dx + 0.5f) / sigmaX2) - erf((dx - 0.5f) / sigmaX2));
                float Ey = 0.5f * (erf((dy + 0.5f) / sigmaY2) - erf((dy - 0.5f) / sigmaY2));
                v += intensity * Ex * Ey;
            }
        }
        pixels[p] = v;
    }

    /*
    * The following code is adapted from
    * http://www.johndcook.com/blog/2009/01/19/stand-alone-error-function-erf/
    * which is licensed Public Domain.
    *
    * Limited precision, only useful for floats. Maximum error is below 1.5 × 10-7.
    */
    float erf(float g) {
        float x = abs(g);
        if (x >= 4.0f)
            return (g > 0.0f) ? 1.0f : -1.0f;

        // constants
        float a1 =  0.254829592f;
        float a2 = -0.284496736f;
        float a3 =  1.421413741f;
        float a4 = -1.453152027f;
        float a5 =  1.061405429f;
        float p  =  0.3275911f;

        // A&S formula 7.1.26
        float t = 1.0f / (1.0f + p*x);
        float y = 1.0f - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*exp(-x*x);

        return (g > 0.0f) ? y : -y;
    }
}