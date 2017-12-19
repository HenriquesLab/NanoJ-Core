package nanoj.core.java.localisation.peakDetection;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import nanoj.core.java.image.filtering.Convolve;
import nanoj.core.java.localisation.particlesHandling.ParticlesHolder;
import nanoj.core.java.localisation.particlesHandling.SimulateStackWithParticles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 13/12/14
 * Time: 20:46
 */
public class SmoothPeakFinderTest {
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
        SimulateStackWithParticles sim_part = new SimulateStackWithParticles();
        ImagePlus imp = sim_part.generateStackWithRandomParticles(64, 64, 100,
                10, 0, 1000, 0, 1.5f, 0, 1.5f, 0, 50, 500, true);

        ParticlesHolder peaks = pf.findPeaks(imp.getImageStack(), 5, 15);

        String error = peaks.getDetectionErrorString(sim_part.getParticlesHolder(), 1, 0, 0);
        System.out.println(error);

    }
}
