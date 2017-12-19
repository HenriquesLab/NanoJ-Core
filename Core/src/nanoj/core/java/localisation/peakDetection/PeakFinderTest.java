package nanoj.core.java.localisation.peakDetection;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.process.FloatProcessor;
import nanoj.core.java.image.filtering.ConvolutionKernels;
import nanoj.core.java.image.filtering.Convolve;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/12/13
 * Time: 5:05 PM
 */
public class PeakFinderTest {
    ImagePlus imp;
    SmoothPeakFinder pf = new SmoothPeakFinder();
    Convolve conv = new Convolve();
    FloatProcessor kernel_DoG;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testFindPeaks() throws Exception {
        int nSlices = 10;
        int nParticles = 10;
        float particle_sigma = 1.5f;
        nanoj.core.java.localisation.particlesHandling.SimulateStackWithParticles sim_part = new nanoj.core.java.localisation.particlesHandling.SimulateStackWithParticles();
        nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks;

        if (true) {
            //with noise
            imp = sim_part.generateStackWithRandomParticles(128, 128, nSlices,
                    nParticles, 0, 1000, 0, particle_sigma, 0, particle_sigma, 0, 50, 500, true);
            kernel_DoG = ConvolutionKernels.genDifferenceOfGaussiansKernel(particle_sigma / 2, particle_sigma * 2);
            conv.convolve2DStack(imp, kernel_DoG);
            peaks = pf.findPeaks(imp.getImageStack(), 2, 5);
        }
        else {
            // without noise
            imp = sim_part.generateStackWithRandomParticles(128, 128, nSlices,
                    nParticles, 0, 1000, 0, particle_sigma, 0, particle_sigma, 0, 0, 500, false);
            peaks = pf.findPeaks(imp.getImageStack(), 2, 0);
        }

        //ParticlesHolder peaks = sim_part.getParticlesHolder();
        nanoj.core.java.localisation.particlesHandling.ParticlesHolder framePeaks;

        if (true)
        {
            imp.show();
            IJ.run(imp, "In [+]", "");
            IJ.run(imp, "In [+]", "");
            IJ.run(imp, "In [+]", "");
            IJ.run(imp, "In [+]", "");
            for (int n=0; n<nSlices;n++)
            {
                framePeaks = peaks.duplicateForTimePoints(n);
                System.out.println("In frame "+n+ " detected nPeaks="+framePeaks.npoints);

                System.out.println(framePeaks.getDataArray("x", false)[0]);

                imp.setSlice(n+1);
                PointRoi roi = new PointRoi(framePeaks.toFloatPolygon());
                imp.setRoi(roi, true);
                sleep(500);
            }
        }
        String error = peaks.getDetectionErrorString(sim_part.getParticlesHolder(), 1, 0, 0);
        System.out.println(error);
        assert (((float) peaks.npoints)/(nSlices*nParticles) > 0.8);
    }
}
