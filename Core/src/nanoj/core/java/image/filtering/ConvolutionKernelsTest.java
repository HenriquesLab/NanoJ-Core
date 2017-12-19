package nanoj.core.java.image.filtering;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * ConvolutionKernels Tester.
 *
 * @author <Authors name>
 * @since <pre>Jan 22, 2015</pre>
 * @version 1.0
 */
public class ConvolutionKernelsTest {

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    /**
     *
     * Method: genGaussianKernel(double sigma)
     *
     */
    @Test
    public void testGenGaussianKernelSigma() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genGaussianKernel(double sigma, float xDelta, float yDelta)
     *
     */
    @Test
    public void testGenGaussianKernelForSigmaXDeltaYDelta() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genLaplacianOfGaussianKernel(double sigma)
     *
     */
    @Test
    public void testGenLaplacianOfGaussianKernel() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genDifferenceOfGaussiansKernel(double sigmaSmall, double sigmaBig)
     *
     */
    @Test
    public void testGenDifferenceOfGaussiansKernel() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genSmooth(int xSize, int ySize)
     *
     */
    @Test
    public void testGenSmooth() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genLaplacian1()
     *
     */
    @Test
    public void testGenLaplacian1() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genLaplacian2()
     *
     */
    @Test
    public void testGenLaplacian2() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genLaplacian3()
     *
     */
    @Test
    public void testGenLaplacian3() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genExtendedLaplacian()
     *
     */
    @Test
    public void testGenExtendedLaplacian() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genExtendedLaplacian2()
     *
     */
    @Test
    public void testGenExtendedLaplacian2() throws Exception {
//TODO: Test goes here... 
    }

    /**
     *
     * Method: genGradientX(int xSize, int ySize)
     *
     */
    @Test
    public void testGenGradientX() throws Exception {
        assertTrue(ConvolutionKernels.isNormalizedToZero(ConvolutionKernels.genGradientX(4, 2), 0.05f));
    }

    /**
     *
     * Method: genGradientY(int xSize, int ySize)
     *
     */
    @Test
    public void testGenGradientY() throws Exception {
        assertTrue (ConvolutionKernels.isNormalizedToZero(ConvolutionKernels.genGradientY(5,5), 0.05f));
    }

    /**
     *
     * Method: gen3DTemporalFrameSubtraction(int xSize, int ySize, int innerTRadius, int outerTDelta)
     *
     */
    @Test
    public void testGen3DTemporalFrameSubtraction() throws Exception {
        assertTrue(ConvolutionKernels.isNormalizedToOne(ConvolutionKernels.gen3DTemporalFrameSubtraction(4, 4, 2, 4), 0.05f));
    }
} 
