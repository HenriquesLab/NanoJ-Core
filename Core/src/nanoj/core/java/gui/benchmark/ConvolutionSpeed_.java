package nanoj.core.java.gui.benchmark;

import com.amd.aparapi.Kernel;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.Convolver;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.filtering.Convolve;
import nanoj.core.java.tools.Log;

import java.io.IOException;

import static nanoj.core.java.image.filtering.ConvolutionKernels.genSmooth;
import static nanoj.core.java.tools.math.Randomizer.addGaussianNoise;

/**
 * Created by Henriques-lab on 11/08/2016.
 */
public class ConvolutionSpeed_ extends _BaseDialog_ {

    Log log = new Log();
    private static final Convolver cv = new Convolver();
    private static final Convolve njcv = new Convolve();
    ResultsTable rt;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = false;
        useSettingsObserver = false;
        return true;
    }

    @Override
    public void setupDialog() {

    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        rt = new ResultsTable();

//        benchmarkConvolution(128, 128, 1, 3, 3, 3);
//        benchmarkConvolution(128, 128, 1, 5, 5, 3);
//        benchmarkConvolution(128, 128, 1, 9, 9, 3);
//
//        benchmarkConvolution(128, 128, 10, 3, 3, 3);
//        benchmarkConvolution(128, 128, 10, 5, 5, 3);
//        benchmarkConvolution(128, 128, 10, 9, 9, 3);
//
//        benchmarkConvolution(128, 128, 100, 3, 3, 3);
//        benchmarkConvolution(128, 128, 100, 5, 5, 3);
//        benchmarkConvolution(128, 128, 100, 9, 9, 3);
//
//        benchmarkConvolution(128, 128, 1000, 3, 3, 3);
//        benchmarkConvolution(128, 128, 1000, 5, 5, 3);
//        benchmarkConvolution(128, 128, 1000, 9, 9, 3);
//
//        benchmarkConvolution(512, 512, 1, 3, 3, 3);
//        benchmarkConvolution(512, 512, 1, 5, 5, 3);
//        benchmarkConvolution(512, 512, 1, 9, 9, 3);
//
//        benchmarkConvolution(512, 512, 10, 3, 3, 3);
//        benchmarkConvolution(512, 512, 10, 5, 5, 3);
//        benchmarkConvolution(512, 512, 10, 9, 9, 3);
//
//        benchmarkConvolution(512, 512, 100, 3, 3, 3);
//        benchmarkConvolution(512, 512, 100, 5, 5, 3);
//        benchmarkConvolution(512, 512, 100, 9, 9, 3);

        benchmarkConvolution(512, 512, 610, 3, 3, 1);
        benchmarkConvolution(512, 512, 610, 3, 3, 1);
        benchmarkConvolution(512, 512, 615, 3, 3, 1);

//        benchmarkConvolution(2560, 2560, 1, 3, 3, 3);
//        benchmarkConvolution(2560, 2560, 1, 5, 5, 3);
//        benchmarkConvolution(2560, 2560, 1, 9, 9, 3);
//
//        benchmarkConvolution(2560, 2560, 10, 3, 3, 3);
//        benchmarkConvolution(2560, 2560, 10, 5, 5, 3);
//        benchmarkConvolution(2560, 2560, 10, 9, 9, 3);
//
//        benchmarkConvolution(2560, 2560, 100, 3, 3, 3);
//        benchmarkConvolution(2560, 2560, 100, 5, 5, 3);
//        benchmarkConvolution(2560, 2560, 100, 9, 9, 3);
//
//        benchmarkConvolution(2560, 2560, 1000, 3, 3, 3);
//        benchmarkConvolution(2560, 2560, 1000, 5, 5, 3);
//        benchmarkConvolution(2560, 2560, 1000, 9, 9, 3);
    }


    private void benchmarkConvolution(int w, int h, int nSlices, int kw, int kh, int iterations) {

        ImageStack ims, imsOriginal = generateStack(w, h, nSlices);
        FloatProcessor kernel = genSmooth(kw, kh);
        float[] kernelArray = (float[]) kernel.getPixels();
        int nPixels = w*h*nSlices;

        double tStart, tTaken, tTakenAverage, pixelsPerMicroSecond;

        // native
        ims = imsOriginal.duplicate();
        tTakenAverage = 0;
        for (int i=0;i<iterations; i++) {
            tStart = System.nanoTime();
            for (int t = 0; t < nSlices; t++) {
                cv.convolve(ims.getProcessor(t + 1), kernelArray, kw, kh);
            }
            tTaken = (System.nanoTime() - tStart); //in nseconds
            tTakenAverage += tTaken/ iterations;
        }
        pixelsPerMicroSecond = nPixels / (tTakenAverage/1000f);
        log.msg("Time with ImageJ [" + w + "x" + h + "x" + nSlices + " kernel " + kw + "x" + kh + "]:" + pixelsPerMicroSecond + " pixel/us");
        rt.incrementCounter();
        rt.addValue("Method", "ImageJ");
        rt.addValue("Image-Width", w);
        rt.addValue("Image-Height", h);
        rt.addValue("Image-Slices", nSlices);
        rt.addValue("Kernel-Width", kw);
        rt.addValue("Kernel-Height", kh);
        rt.addValue("PixelsPerMicrosecond", pixelsPerMicroSecond);

        // nanoj jtp
        ims = imsOriginal.duplicate();
        NJKernel.NANOJ_EXECUTION_Mode = Kernel.EXECUTION_MODE.JTP;
        tTakenAverage = 0;
        for (int i=0;i<iterations; i++) {
            tStart = System.nanoTime();
            njcv.convolve2DStack(ims, kernel);
            tTaken = (System.nanoTime() - tStart); //in nseconds
            tTakenAverage += tTaken/ iterations;
        }
        pixelsPerMicroSecond = nPixels / (tTakenAverage/1000f);
        log.msg("Time with NanoJ-JTP [" + w + "x" + h + "x" + nSlices + " kernel " + kw + "x" + kh + "]:" + pixelsPerMicroSecond + " pixel/us");
        rt.incrementCounter();
        rt.addValue("Method", "NanoJ-JTP");
        rt.addValue("Image-Width", w);
        rt.addValue("Image-Height", h);
        rt.addValue("Image-Slices", nSlices);
        rt.addValue("Kernel-Width", kw);
        rt.addValue("Kernel-Height", kh);
        rt.addValue("PixelsPerMicrosecond", pixelsPerMicroSecond);

        // nanoj CL
        ims = imsOriginal.duplicate();
        NJKernel.NANOJ_EXECUTION_Mode = Kernel.EXECUTION_MODE.GPU;
        tTakenAverage = 0;
        for (int i=0;i<iterations; i++) {
            tStart = System.nanoTime();
            njcv.convolve2DStack(ims, kernel);
            tTaken = (System.nanoTime() - tStart); //in nseconds
            tTakenAverage += tTaken/ iterations;
        }
        pixelsPerMicroSecond = nPixels / (tTakenAverage/1000f);
        log.msg("Time with NanoJ-GPU [" + w + "x" + h + "x" + nSlices + " kernel " + kw + "x" + kh + "]:" + pixelsPerMicroSecond + " pixel/us");
        rt.incrementCounter();
        rt.addValue("Method", "NanoJ-GPU");
        rt.addValue("Image-Width", w);
        rt.addValue("Image-Height", h);
        rt.addValue("Image-Slices", nSlices);
        rt.addValue("Kernel-Width", kw);
        rt.addValue("Kernel-Height", kh);
        rt.addValue("PixelsPerMicrosecond", pixelsPerMicroSecond);

        //rt.updateResults();
        rt.show("Benchmark Results");
    }

    private ImageStack generateStack(int w, int h, int nSlices) {
        ImageStack ims = new ImageStack(w, h);

        for (int s=0; s<nSlices; s++) {
            FloatProcessor fp = new FloatProcessor(w, h);
            addGaussianNoise(fp, 1000, 100);
            ims.addSlice(fp);
        }
        return ims;
    }
}
