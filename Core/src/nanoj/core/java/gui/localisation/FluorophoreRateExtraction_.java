package nanoj.core.java.gui.localisation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.measure.CurveFitter;
import ij.plugin.StackCombiner;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.featureExtraction.ExtractRois;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.filtering.Convolve;
import nanoj.core.java.io.LoadNanoJTable;
import nanoj.kernels.Kernel_CalculatePeaks;
import nanoj.kernels.Kernel_NearestNeighbour;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static ij.plugin.HyperStackConverter.toHyperStack;
import static ij.plugin.frame.Fitter.plot;
import static java.lang.Math.*;
import static nanoj.core.java.array.ArrayCasting.floatToDouble;
import static nanoj.core.java.array.ArrayCasting.toArray;
import static nanoj.core.java.array.ArrayInitialization.initializeFloatAndGrowthFill;
import static nanoj.core.java.array.ArrayMath.getBackgroundMeanAndStdDev;
import static nanoj.core.java.image.filtering.ConvolutionKernels.genCircularIntegration;
import static nanoj.core.java.io.OpenNanoJDataset.openNanoJDataset;

/**
 * Created by sculley on 24/02/2016.
 */
public class FluorophoreRateExtraction_ extends _BaseDialog_ {
    private Kernel_NearestNeighbour NN = new Kernel_NearestNeighbour();
    private static Kernel_CalculatePeaks kernelCalculatePeaks = new Kernel_CalculatePeaks();
    private Convolve cv = new Convolve();
    private ImagePlus impProjection = null;
    private double fwhm, snr;
    private int nProjFrames, filterBrightest, filterDimmest;
    private boolean previewMaxima, doDriftCorrection, doBatch;
    private PointRoi plgMax;
    private PointRoi plgMin;
    private float[] xMaxCoordinates, yMaxCoordinates, xMinCoordinates, yMinCoordinates;
    private FloatProcessor integrationKernel;
    private Map<String, double[]> driftTable = null;
    private double[] driftX, driftY;
    private String filePath, projectionType;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = false;
        useSettingsObserver = true;
        driftTable = null;

        return true;
    }

    @Override
    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Rate Extraction...");
        gd.addNumericField("Minimum SNR (default: 10)", getPrefs("snr", 10), 2);
        gd.addNumericField("PSF FWHM (default: 3)", getPrefs("fwhm", 3), 2);
        gd.addNumericField("Initial number of frames projected for probe detection (default: 1000)", getPrefs("nProjFrames", 1000), 0);
        gd.addChoice("Projections type", new String[] {"Max", "StdDev"}, getPrefs("projectionType", "StdDev"));

        gd.addNumericField("Filter out brightest (default: 10 percent)", getPrefs("filterBrightest", 10), 0);
        gd.addNumericField("Filter out dimmest (default: 10 percent)", getPrefs("filterDimmest", 10), 0);

        gd.addCheckbox("Do_batch-analysis (.nji files in selected folder)", getPrefs("doBatch", false));
        gd.addCheckbox("Do drift correction (default: active)", getPrefs("doDriftCorrection", false));
        gd.addCheckbox("Show Maxima (Minima otherwise)", true);
        gd.addCheckbox("Show preview", false);
    }

    @Override
    public boolean loadSettings() {
        snr = gd.getNextNumber();
        fwhm = gd.getNextNumber();
        nProjFrames = (int) gd.getNextNumber();
        projectionType = gd.getNextChoice();
        filterBrightest = (int) gd.getNextNumber();
        filterDimmest = (int) gd.getNextNumber();
        doBatch = gd.getNextBoolean();
        doDriftCorrection = gd.getNextBoolean();
        previewMaxima = gd.getNextBoolean();
        showPreview = gd.getNextBoolean();

        integrationKernel = genCircularIntegration(fwhm);

        setPrefs("snr", snr);
        setPrefs("fwhm", fwhm);
        setPrefs("doBatch", doBatch);
        setPrefs("doDriftCorrection", doDriftCorrection);
        setPrefs("nProjFrames", nProjFrames);
        setPrefs("projectionType", projectionType);
        setPrefs("filterBrightest", filterBrightest);
        setPrefs("filterDimmest", filterDimmest);

        savePrefs();
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        if (doBatch) {
            if (imp != null) imp.close();
            String batchFolderPath = IJ.getDir("Folder with .nji datasets...");

            for (File f : new File(batchFolderPath).listFiles()) {
                if (!prefs.continueNanoJCommand()) {
                    log.abort();
                    return;
                }

                driftTable = null;
                if (f.getName().endsWith("SRRF-000.nji")) {
                    filePath = f.getPath().replace("SRRF-000.nji", "");
                    //if (new File(filePath+"TimeTraces.tif").exists()) continue;

                    // do analysis
                    impPath = f.getPath();
                    imp = openNanoJDataset(impPath);
                    imp.show();

                    log.msg("Starting analysis of: " + f.getPath());
                    runAnalysis(imp);
                    imp.close();
                }
            }
        }
        else {
            if (imp == null) openImp();
            runAnalysis(imp);
        }
    }

    public void runAnalysis(ImagePlus imp){
        nProjFrames = Math.min(nProjFrames, imp.getImageStackSize());

        loadDriftTable(imp.getImageStackSize());

        if (impProjection == null) calculateProjection();
        calculateMaximaAndMinima();

        ImageStack ims = imp.getImageStack();

        int w = imp.getWidth();
        int h = imp.getHeight();
        int nPeaks = xMaxCoordinates.length;
        int nTimePoints = ims.getSize();

        RoiManager rm = ExtractRois.getRoiManager();
        for (int p=0; p<xMaxCoordinates.length; p++) {
            rm.addRoi(new PointRoi(xMaxCoordinates[p], yMaxCoordinates[p]));
        }
        rm.runCommand(imp, "Remove Slice Info");
        rm.runCommand(imp, "Remove Frame Info");

        float[][] timeTraces = new float[nPeaks][nTimePoints];
        float[] background = new float[nTimePoints];

        for (int t=1; t<=ims.getSize(); t++) {
            log.status("Getting time-traces... processing time-point " + t + "/" + nTimePoints);
            log.progress(t, ims.getSize());
            FloatProcessor fp = ims.getProcessor(t).convertToFloatProcessor();
            //fp = cv.convolve2D(fp, integrationKernel);
            fp.setInterpolationMethod(ImageProcessor.BICUBIC);

            for (int p = 0; p < xMinCoordinates.length; p++) {
                double x = (((int) xMinCoordinates[p]) + driftX[t-1]);
                double y = (((int) yMinCoordinates[p]) + driftY[t-1]);
                double v = fp.getInterpolatedPixel(x, y);
                background[t - 1] += (v - background[t - 1]) / (p + 1);
            }

            for (int p = 0; p < nPeaks; p++) {
                double x = (((int) xMaxCoordinates[p]) + driftX[t-1]);
                double y = (((int) yMaxCoordinates[p]) + driftY[t-1]);
                double v = fp.getInterpolatedPixel(x, y);
                timeTraces[p][t - 1] = (float) (v - background[t - 1]);
            }
        }

        log.status("Binning time-traces...");
        float[][] binaryTimeTraces = new float[nPeaks][nTimePoints];

        double[] photonFlux_ONState = new double[nPeaks];
        double[] photonFlux_ONState_stdDev = new double[nPeaks];
        int[] photonFlux_ONState_counter = new int[nPeaks];
        double[] photonFlux_OFFState = new double[nPeaks];
        double[] photonFlux_OFFState_stdDev = new double[nPeaks];
        int[] photonFlux_OFFState_counter = new int[nPeaks];

        double[] photonFlux_ONState_100 = new double[nPeaks];
        double[] photonFlux_ONState_stdDev_100 = new double[nPeaks];
        int[] photonFlux_ONState_counter_100 = new int[nPeaks];
        double[] photonFlux_OFFState_100 = new double[nPeaks];
        double[] photonFlux_OFFState_stdDev_100 = new double[nPeaks];
        int[] photonFlux_OFFState_counter_100 = new int[nPeaks];

        int[] ON_counter = new int[nTimePoints];

        FloatProcessor fpTimeTraces = new FloatProcessor(timeTraces);
        double[] meanAndStdDev = getBackgroundMeanAndStdDev((float[]) fpTimeTraces.getPixels(), 0.5f, false);
        double mean = meanAndStdDev[0];
        double stdDev = meanAndStdDev[1];
//        double mean = getAverageValue((float[]) fpTimeTraces.getPixels());
//        double stdDev = getStandardDeviationValue((float[]) fpTimeTraces.getPixels());

        for (int p = 0; p < nPeaks; p++) {
            log.progress(p+1, nPeaks);

//            double mean = getAverageValue(timeTraces[p]);
//            double stdDev = getStandardDeviationValue(timeTraces[p]);
//            float[][] averageAndStdDev = movingAverageAndStdDev(timeTraces[p], 100);
//            float[] mean = averageAndStdDev[0];
//            float[] stdDev = averageAndStdDev[1];

            for (int t=0; t<nTimePoints; t++) {

                if (timeTraces[p][t] - mean > snr*stdDev) {
                    // make binary track
                    binaryTimeTraces[p][t] = 1;
                    ON_counter[t]++;

                    // calculate photonFlux ON-state
                    photonFlux_ONState_counter[p]++;
                    double v = timeTraces[p][t];
                    double oldFlux = photonFlux_ONState[p];
                    double newFlux = oldFlux + (v - oldFlux) / photonFlux_ONState_counter[p];
                    double delta = (v - newFlux) * (v - oldFlux);
                    photonFlux_ONState_stdDev[p] += (delta - photonFlux_ONState_stdDev[p]) / photonFlux_ONState_counter[p];
                    photonFlux_ONState[p] = newFlux;

                    // calculate photonFlux ON-state first 100 frames
                    if (t<100) {
                        photonFlux_ONState_counter_100[p]++;
                        double v_100 = timeTraces[p][t];
                        double oldFlux_100 = photonFlux_ONState_100[p];
                        double newFlux_100 = oldFlux_100 + (v_100 - oldFlux_100) / photonFlux_ONState_counter_100[p];
                        double delta_100 = (v_100 - newFlux_100) * (v_100 - oldFlux_100);
                        photonFlux_ONState_stdDev_100[p] += (delta_100 - photonFlux_ONState_stdDev_100[p]) / photonFlux_ONState_counter_100[p];
                        photonFlux_ONState_100[p] = newFlux_100;
                    }
                }
                else {
                    // calculate photonFlux OFF-state
                    photonFlux_OFFState_counter[p]++;
                    double v = timeTraces[p][t];
                    double oldFlux = photonFlux_OFFState[p];
                    double newFlux = oldFlux + (v - oldFlux) / photonFlux_OFFState_counter[p];
                    double delta = (v - newFlux) * (v - oldFlux);
                    photonFlux_OFFState_stdDev[p] += (delta - photonFlux_OFFState_stdDev[p]) / photonFlux_OFFState_counter[p];
                    photonFlux_OFFState[p] = newFlux;

                    // calculate photonFlux ON-state first 100 frames
                    if (t<100) {
                        photonFlux_OFFState_counter_100[p]++;
                        double v_100 = timeTraces[p][t];
                        double oldFlux_100 = photonFlux_OFFState_100[p];
                        double newFlux_100 = oldFlux_100 + (v_100 - oldFlux_100) / photonFlux_OFFState_counter_100[p];
                        double delta_100 = (v_100 - newFlux_100) * (v_100 - oldFlux_100);
                        photonFlux_OFFState_stdDev_100[p] += (delta_100 - photonFlux_OFFState_stdDev_100[p]) / photonFlux_OFFState_counter_100[p];
                        photonFlux_OFFState_100[p] = newFlux_100;
                    }
                }
            }
        }
        for (int p=0; p<nPeaks; p++) {
            photonFlux_ONState_stdDev[p] = sqrt(photonFlux_ONState_stdDev[p]);
            photonFlux_ONState_stdDev_100[p] = sqrt(photonFlux_ONState_stdDev_100[p]);
            photonFlux_OFFState_stdDev[p] = sqrt(photonFlux_OFFState_stdDev[p]);
            photonFlux_OFFState_stdDev_100[p] = sqrt(photonFlux_OFFState_stdDev_100[p]);
        }
        double[] ONRatio = new double[nTimePoints];
        for (int t=0; t<nTimePoints; t++) {
            ONRatio[t] = ON_counter[t]/((double) nPeaks);
        }

        int ONSTATE = 1;
        int OFFSTATE = 0;
        int BLEACHSTATE = -1;

        ArrayList<Float> ONEvents = new ArrayList<Float>();
        ArrayList<Float> OFFEvents = new ArrayList<Float>();
        ArrayList<Float> BLEACHEvents = new ArrayList<Float>();
        int[] cycles = new int[binaryTimeTraces.length];

        for (int p=0; p<nPeaks; p++) {
            log.status("Analysing binned time-traces...");
            log.progress(p+1, nPeaks);

            int previousState = ONSTATE;
            if (binaryTimeTraces[p][0] == 0) previousState = OFFSTATE;
            float stateCounter = 0;

            for (int t=0; t<binaryTimeTraces[0].length; t++) {

                int currentState = (int) binaryTimeTraces[p][t];

                if (previousState == ONSTATE && currentState == OFFSTATE) {
                    ONEvents.add(stateCounter);
                    stateCounter = 0;
                }
                else if (previousState == OFFSTATE && currentState == ONSTATE) {
                    OFFEvents.add(stateCounter);
                    stateCounter = 0;
                    cycles[p]++;
                }

                stateCounter++;
                previousState = currentState;
            }

            int currentState = (int) binaryTimeTraces[p][binaryTimeTraces[0].length-1];
            if (currentState == OFFSTATE)
                BLEACHEvents.add(stateCounter);
        }

        ImageStack imsTraces = new ImageStack(timeTraces.length, timeTraces[0].length);
        imsTraces.addSlice(fpTimeTraces);
        imsTraces.addSlice(new FloatProcessor(binaryTimeTraces));
        ImagePlus impTraces = new ImagePlus("Time-Traces", imsTraces);
        impTraces = toHyperStack(impTraces, 2, 1, 1, "xyztc", "Composite");
        impTraces.setTitle(imp.getTitle()+" - Time-Traces");
        if (!doBatch) impTraces.show();
        else IJ.saveAsTiff(impTraces, filePath+"TimeTraces");

        float[] _ONEvents = toArray(ONEvents, 0f);
        float[] _OFFEvents = toArray(OFFEvents, 0f);
        float[] _BLEACHEvents = toArray(BLEACHEvents, 0f);

        ImagePlus impOnTimes = new ImagePlus("on times", new FloatProcessor(_ONEvents.length, 1, _ONEvents));
        ImagePlus impOffTimes = new ImagePlus("off times", new FloatProcessor(_OFFEvents.length, 1, _OFFEvents));
        ImagePlus impCycles = new ImagePlus("cycles", new FloatProcessor(cycles.length, 1, cycles));

        HistogramWindow histOnTime = new HistogramWindow("ON-Time Histogram",impOnTimes, 15, 1, 15);
        HistogramWindow histOffTime = new HistogramWindow("OFF-Time Histogram",impOffTimes, 30, 1, 30);
        HistogramWindow histCycles = new HistogramWindow("Cycles Histogram",impCycles, 100);

        StackCombiner SC = new StackCombiner();

        ImageStack imsStats;
        imsStats = SC.combineVertically(histOnTime.getImagePlus().getImageStack(), histOffTime.getImagePlus().getImageStack());
        imsStats = SC.combineVertically(imsStats, histCycles.getImagePlus().getImageStack());

        // see Jungmann, Nano Letters, 2010

        CurveFitter cf;

        // mono exponential fit
        cf = new CurveFitter(histOnTime.getResultsTable().getColumnAsDoubles(0), histOnTime.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = b * exp(-x/a)", new double[]{1, 100}, false);
        plot(cf);
        impOnTimes = IJ.getImage();

        cf = new CurveFitter(histOffTime.getResultsTable().getColumnAsDoubles(0), histOffTime.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = b * exp(-x/a)", new double[]{1, 100}, false);
        plot(cf);
        impOffTimes = IJ.getImage();

        cf = new CurveFitter(histCycles.getResultsTable().getColumnAsDoubles(0), histCycles.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = b * exp(-x/a)", new double[]{1, 100}, false);
        plot(cf);
        impCycles = IJ.getImage();

        ImageStack imsStats2 = SC.combineVertically(impOnTimes.getStack(), impOffTimes.getStack());
        imsStats2 = SC.combineVertically(imsStats2, impCycles.getStack());

        impOnTimes.close();
        impOffTimes.close();
        impCycles.close();

        // bi exponential fit
        cf = new CurveFitter(histOnTime.getResultsTable().getColumnAsDoubles(0), histOnTime.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = c * exp(-x/a) + d * exp(-x/b)", new double[]{1, 100, 1, 100}, false);
        plot(cf);
        impOnTimes = IJ.getImage();

        cf = new CurveFitter(histOffTime.getResultsTable().getColumnAsDoubles(0), histOffTime.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = c * exp(-x/a) + d * exp(-x/b)", new double[]{1, 100, 1, 100}, false);
        plot(cf);
        impOffTimes = IJ.getImage();

        cf = new CurveFitter(histCycles.getResultsTable().getColumnAsDoubles(0), histCycles.getResultsTable().getColumnAsDoubles(1));
        cf.doCustomFit("y = c * exp(-x/a) + d * exp(-x/b)", new double[]{1, 100, 1, 100}, false);
        plot(cf);
        impCycles = IJ.getImage();

        ImageStack imsStats3 = SC.combineVertically(impOnTimes.getStack(), impOffTimes.getStack());
        imsStats3 = SC.combineVertically(imsStats3, impCycles.getStack());

        imsStats = SC.combineHorizontally(imsStats, imsStats2);
        imsStats = SC.combineHorizontally(imsStats, imsStats3);

        impOnTimes.close();
        impOffTimes.close();
        impCycles.close();
        histOnTime.close();
        histOffTime.close();
        histCycles.close();

        // create plots for photon flux
        double[] xAxis = floatToDouble(initializeFloatAndGrowthFill(nPeaks, 1f, 1f));
        Plot photonFluxPlot_ONState = new Plot("Photon Flux - ON-State", "molecule", "ON-state (photons/frame)");
        photonFluxPlot_ONState.addPoints(xAxis, photonFlux_ONState, photonFlux_ONState_stdDev, Plot.CROSS);
        //photonFluxPlot_ONState.setLogScaleY();
        //photonFluxPlot_ONState.setLimits(0, nPeaks, 0, 100000);


        Plot photonFluxPlot_ONState_100 = new Plot("Photon Flux - ON-State", "molecule", "First 100f ON-state (photons/frame)");
        photonFluxPlot_ONState_100.addPoints(xAxis, photonFlux_ONState_100, photonFlux_ONState_stdDev_100, Plot.CROSS);
        //photonFluxPlot_ONState_100.setLogScaleY();
        //photonFluxPlot_ONState_100.setLimits(0, nPeaks, 0, 100000);

        Plot photonFluxPlot_OFFState = new Plot("Photon Flux - OFF-State", "molecule", "OFF-state (photons/frame)");
        photonFluxPlot_OFFState.addPoints(xAxis, photonFlux_OFFState, photonFlux_OFFState_stdDev, Plot.CROSS);

        Plot photonFluxPlot_OFFState_100 = new Plot("Photon Flux - OFF-State", "molecule", "First 100f OFF-state (photons/frame)");
        photonFluxPlot_OFFState_100.addPoints(xAxis, photonFlux_OFFState_100, photonFlux_OFFState_stdDev_100, Plot.CROSS);

        xAxis = floatToDouble(initializeFloatAndGrowthFill(nTimePoints, 1f, 1f));
        Plot ONRatioPlot = new Plot("ON-ratio", "time-point", "ON-fraction", xAxis, ONRatio);

        ImageStack imsStats4 = SC.combineVertically(photonFluxPlot_ONState.getImagePlus().getStack(), photonFluxPlot_OFFState.getImagePlus().getStack());
        imsStats4 = SC.combineVertically(imsStats4, ONRatioPlot.getImagePlus().getStack());
        ImageStack imsStats5 = SC.combineVertically(photonFluxPlot_ONState_100.getImagePlus().getStack(), photonFluxPlot_OFFState_100.getImagePlus().getStack());

        imsStats = SC.combineHorizontally(imsStats, imsStats4);
        imsStats = SC.combineHorizontally(imsStats, imsStats5);

        ImagePlus impBlinkingStatistics = new ImagePlus(imp.getTitle()+"- Blinking Statistics", imsStats);
        if (!doBatch) impBlinkingStatistics.show();
        else {
            if (impProjection != null) {
                impProjection.close();
                impProjection = null;
            }
            IJ.saveAsTiff(impBlinkingStatistics, filePath+"BlinkingStatistics");
        }
    }

    public void doPreview() {
        if (imp == null) {
            openImp();
            filePath = impPath;
        }

        loadDriftTable(imp.getImageStackSize());
        if (impProjection == null) calculateProjection();
        calculateMaximaAndMinima();
        impProjection.show();

        PointRoi roi;
        if (previewMaxima) roi = plgMax;
        else roi = plgMin;

        impProjection.setRoi(roi);
    }

    private void calculateProjection() {
        ImageStack ims = imp.getImageStack();
        FloatProcessor fpProjection = new FloatProcessor(ims.getWidth(), ims.getHeight());
        float[] pixelsProjection = (float[]) fpProjection.getPixels();
        float[] pixelsProjectionMean = new float[pixelsProjection.length];

        int nSlices = Math.min(ims.getSize(), nProjFrames);

        for (int s=1; s<=nSlices; s++) {
            log.status("Calculating projection... "+s+"/"+nSlices);
            log.progress(s, nSlices);
            FloatProcessor fpSlice = (FloatProcessor) ims.getProcessor(s).convertToFloat();
            //fpSlice = cv.convolve2D(fpSlice, integrationKernel);
            if (driftX[s-1]!=0 && driftY[s-1]!=0) {
                fpSlice.setInterpolationMethod(ImageProcessor.BICUBIC);
                fpSlice.translate(driftX[s-1], driftY[s-1]);
            }

            float[] pixelsSlice = (float[]) fpSlice.getPixels();

            if (projectionType.equals("Max")) {
                for (int p = 0; p < pixelsSlice.length; p++) {
                    pixelsProjection[p] = Math.max(pixelsProjection[p], pixelsSlice[p]);
                }
            }
            else if (projectionType.equals("StdDev")) {
                for (int p = 0; p < pixelsSlice.length; p++) {
                    float v = pixelsSlice[p];
                    float oldMean = pixelsProjectionMean[p];
                    float newMean = oldMean + (v - oldMean) / s;
                    pixelsProjectionMean[p] = newMean;

                    float delta = (v - newMean) * (v - oldMean);
                    pixelsProjection[p] += (delta - pixelsProjection[p]) / s;
                }
            }
        }
        if (projectionType.equals("StdDev")) {
            for (int p = 0; p < pixelsProjection.length; p++)
                pixelsProjection[p] = (float) sqrt(pixelsProjection[p]);
        }

        if (impProjection == null) impProjection = new ImagePlus(projectionType+" Projection", fpProjection);
        else impProjection.setProcessor(fpProjection);
        impProjection.show();
    }

    private void calculateMaximaAndMinima() {
        FloatProcessor ip = impProjection.getProcessor().convertToFloatProcessor();

        kernelCalculatePeaks.calculate(ip, (float) snr);
        //kernelCalculatePeaks.calculate(ip, 0);
        float[][] peaks = kernelCalculatePeaks.getXYPointsSortedByIntensity(0, true);
        //float[][] peaks = kernelCalculatePeaks.getXYPoints(0);
        xMaxCoordinates = peaks[0];
        yMaxCoordinates = peaks[1];
        plgMax = new PointRoi(xMaxCoordinates, yMaxCoordinates, xMaxCoordinates.length);
        //log.msg(""+xCoordinates.length);

        ip.multiply(-1);
        kernelCalculatePeaks.calculate(ip, 0);
        xMinCoordinates = kernelCalculatePeaks.getXPoints(0);
        yMinCoordinates = kernelCalculatePeaks.getYPoints(0);
        plgMin = new PointRoi(xMinCoordinates, yMinCoordinates, xMinCoordinates.length);

        double fwhm2 = fwhm * 2;
        int w = ip.getWidth();
        int h = ip.getHeight();

        ArrayList<Float> _xps = new ArrayList<Float>();
        ArrayList<Float> _yps = new ArrayList<Float>();
        int pStart = round(xMaxCoordinates.length * (filterDimmest/100f));
        int pEnd = round(xMaxCoordinates.length * (1-filterBrightest/100f));
        for (int n = pStart; n < pEnd; n++) {
            _xps.add(xMaxCoordinates[n]);
            _yps.add(yMaxCoordinates[n]);
        }

        int counter;

        // filter peaks that are too close
        counter = 0;
        while (counter < _xps.size()) {
            log.status("Filtering peaks... " + counter + "/" + (_xps.size()-1));

            float x0 = _xps.get(counter);
            float y0 = _yps.get(counter);

            double nearestDistance = Double.MAX_VALUE;
            for (int n = 0; n < _xps.size(); n++) {
                if (n == counter) continue;
                float x1 = _xps.get(n);
                float y1 = _yps.get(n);
                double d = sqrt(pow(x0 - x1, 2) + pow(y0 - y1, 2));
                nearestDistance = Math.min(nearestDistance, d);
            }

            if (nearestDistance > fwhm2 && x0 - fwhm2 > 0 && y0 - fwhm2 > 0 && x0 + fwhm2 < w && y0 + fwhm2 < h) {
                counter++;
            } else {
                _xps.remove(counter);
                _yps.remove(counter);
            }
        }

        xMaxCoordinates = toArray(_xps, 0f);
        yMaxCoordinates = toArray(_yps, 0f);
        plgMax = new PointRoi(xMaxCoordinates, yMaxCoordinates);
    }

    private void loadDriftTable(int nSlices) {
        if (doDriftCorrection && driftTable == null) {

            String tablePath = null;
            if (filePath != null) tablePath = filePath+"DriftTable.njt";
            try {
                if (tablePath!=null && new File(tablePath).exists())
                    driftTable = new LoadNanoJTable(tablePath).getData();
                else
                    driftTable = new LoadNanoJTable(null).getData();

            } catch (IOException e) {
                log.warning("could not load drift table!!");
            }
        }

        if (!doDriftCorrection) {
            driftX = new double[nSlices];
            driftY = new double[nSlices];
        }
        else {
            driftX = driftTable.get("X-Drift (pixels)");
            driftY = driftTable.get("Y-Drift (pixels)");
        }
    }
}
