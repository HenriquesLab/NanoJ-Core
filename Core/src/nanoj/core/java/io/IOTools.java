package nanoj.core.java.io;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/29/13
 * Time: 1:56 PM
 */
public class IOTools {

    public static void saveImageAndParticlesRoi(ImagePlus imp, nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks, String path) throws InterruptedException {

        nanoj.core.java.localisation.particlesHandling.ParticlesHolder framePeaks;
        RoiManager rm = new RoiManager(true);

        //imp.show();
        for (int n=0; n<imp.getNSlices();n++)
        {
            framePeaks = peaks.duplicateForTimePoints(n);

            imp.setSlice(n+1);
            PointRoi roi = new PointRoi(framePeaks.toFloatPolygon());
            imp.setRoi(roi, true);
            rm.add(imp, roi, n);
            //sleep(100);
        }

        rm.runCommand("Deselect");
        rm.runCommand("Save", path+".zip");
        rm.runCommand("Delete");
        rm.runCommand("Open", path+".zip");

        IJ.save(imp, path+".tif");
        //imp.lock();
        //imp.unlock();
        rm.close();
    }

    public static void saveImageAndVector4ParticlesRois(ImagePlus imp, nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks, String path){

        nanoj.core.java.localisation.particlesHandling.ParticlesHolder framePeaks;
        RoiManager rm = new RoiManager();

        imp.show();

        int counter = 0;
        for (int n=0; n<imp.getNSlices();n++)
        {
            framePeaks = peaks.duplicateForTimePoints(n);

            imp.setSlice(n+1);
            Roi [] rois = nanoj.core.java.localisation.peakDetection.RoiTools.vector4ParticlesRois(framePeaks);

            for (Roi roi: rois){
                imp.setRoi(roi, true);
                rm.add(imp, roi, counter);
                counter++;
            }
        }
        rm.runCommand("Deselect");
        rm.runCommand("Save", path+".zip");
        rm.runCommand("Delete");
        rm.runCommand("Open", path+".zip");

        IJ.save(imp, path+".tif");
        //imp.lock();
        //imp.unlock();
        rm.close();
    }

    public static void savePlotRMSDOverFrames(nanoj.core.java.localisation.particlesHandling.ParticlesHolder truePeaks, nanoj.core.java.localisation.particlesHandling.ParticlesHolder[] measuredPeaks,
                                              String x_axis_label, float x_axis_scale, float x_axis_start,
                                              String path)
    {

        int nFrames = truePeaks.getMaxT()+1;
        nanoj.core.java.localisation.particlesHandling.ParticlesHolder framePeaks;
        Map<String, Float> error;

        float [][] rmsd_lateral     = new float[measuredPeaks.length][];
        float [][] detection_recall = new float[measuredPeaks.length][];

        float [] x_axis = new float[nFrames];

        int counter = 0;
        for (nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks: measuredPeaks){
            float [] _rmsd_lateral     = new float[nFrames];
            float [] _detection_recall = new float[nFrames];

            for (int n=0; n<nFrames; n++)
            {
                framePeaks = peaks.duplicateForTimePoints(n);
                error = framePeaks.getDetectionError(truePeaks.duplicateForTimePoints(n), 2, 0, 0);

                _rmsd_lateral[n]     = error.get("RMSD lateral");
                _detection_recall[n] = error.get("detection recall");
                //System.out.println(n+" "+_rmsd_lateral[n]);
            }

            rmsd_lateral[counter]     = _rmsd_lateral;
            detection_recall[counter] = _detection_recall;

            counter++;
        }

        String data = "";
        for (int n=0; n<nFrames; n++)
        {
            data+=""+n;
            for (int p=0; p<rmsd_lateral.length; p++)
            {
                data+="\t"+rmsd_lateral[p][n];
            }
            data+="\n";
        }
        PrintWriter info = null;
        try {
            info = new PrintWriter(path+".csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        info.println(data);
        info.close();


//
//        float [] y_axis = new float[nFrames];
//        for (int n=0; n<nFrames; n++)
//        {
//            //x_axis[n] = x_axis_start+(n*x_axis_scale);
//            x_axis[n] = n;
//        }
//
//        Plot rmsdPlot   = new Plot("RMSD", x_axis_label, "RMSD", x_axis, y_axis, Plot.CROSS);
//        Plot recallPlot = new Plot("Detection recall", x_axis_label, "recall", x_axis, y_axis, Plot.CROSS);
//
//        rmsdPlot.setLimits(0, 500, 0, 2);
//        recallPlot.setLimits(0, 500, 0, 1);
//
//        rmsdPlot.addPoints(x_axis, rmsd_lateral[0], Plot.BOX);
//        rmsdPlot.addPoints(x_axis, rmsd_lateral[1], Plot.CIRCLE);
//
//        for (int n=0; n<rmsd_lateral.length; n++)
//        {
//            //rmsdPlot.addPoints(x_axis, rmsd_lateral[n], Plot.CROSS);
//            recallPlot.addPoints(x_axis, detection_recall[n], Plot.CROSS);
//        }
//
//        rmsdPlot.draw();
//        recallPlot.draw();
//
//        IJ.save(rmsdPlot.getImagePlus(), path+"_rmsd.tif");
//        IJ.save(recallPlot.getImagePlus(), path+"_recall.tif");
    }
}
