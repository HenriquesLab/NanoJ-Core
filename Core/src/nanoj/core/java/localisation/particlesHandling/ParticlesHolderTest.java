package nanoj.core.java.localisation.particlesHandling;

import nanoj.core.java.tools.math.Randomizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/26/13
 * Time: 5:51 PM
 */
public class ParticlesHolderTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDuplicateForTimePoints() throws Exception {
        ParticlesHolder peaks = new ParticlesHolder();

        String [] entryNames = {"signal", "x", "y", "z", "t"};
        float [] entryValues = new float[5];

        for (int t=0; t<10; t++)
        {
            entryValues[0] = 1;
            entryValues[1] = Randomizer.random.nextFloat();
            entryValues[2] = Randomizer.random.nextFloat();
            entryValues[3] = Randomizer.random.nextFloat();
            entryValues[4] = t;
            peaks.addPoint(entryNames, entryValues);
        }

        assert(peaks.duplicateForTimePoints(1).npoints == 1);

    }

}
