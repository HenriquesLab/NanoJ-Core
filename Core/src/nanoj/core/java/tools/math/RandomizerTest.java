package nanoj.core.java.tools.math;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by nils on 06/01/15.
 */


public class RandomizerTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testRandomizer() throws Exception{
        double mean = 80;
        int sampleSize = 10000;
        float[] randomNumbers = new float[sampleSize];
        for(int i = 0; i < randomNumbers.length; i++){
            randomNumbers[i] = (float) Randomizer.poissonValue(mean);
        }
        float Sum = 0;
        float Sum2 = 0;
        float Ave;
        float Var;
        for(int i = 0; i < randomNumbers.length; i++){
            Sum += randomNumbers[i];
            Sum2 += randomNumbers[i] * randomNumbers[i];
        }
        Ave = Sum / ((float)sampleSize);
        Var = (Sum2 - Sum*Sum/((float)sampleSize))/((float)sampleSize - 1);

        System.out.println(Ave);
        System.out.println(Var);

    }

}
