package nanoj.core.java.array;

import java.util.ArrayList;

import static java.lang.Math.pow;

/**
 *
 * Type conversion of arrays
 *
 * @author Henriques Lab
 *
 * Created by Henriques-lab on 10/06/2016.
 */
public class ArrayTypeConversion {

    /**
     * converts float array to integer array. Values multiplied by 10 to the power of precision before conversion
     * @param floatArr
     * @param precision
     * @return
     */
    public static int[] encodeFloatArrayIntoInt(float [] floatArr, int precision){
        int [] intArr = new int[floatArr.length];
        for (int n=0; n<intArr.length; n++) {
            intArr[n] = (int) Math.round(floatArr[n]*pow(10, precision));
        }
        return intArr;
    }

    /**
     * converts integer array to float array. Values divided by 10 to the power of precision after conversion
     * @param intArr
     * @param precision
     * @return
     */
    public static float[] decodeFloatArrayFromInt(int [] intArr, int precision){
        float [] floatArr = new float[intArr.length];
        for (int n=0; n<intArr.length; n++) {
            floatArr[n] = (float) (intArr[n]/pow(10, precision));
        }
        return floatArr;
    }

    /**
     * converts integer array to float array
     * @param intArr
     * @return
     */
    public static float[] intArray2floatArray(int [] intArr){
        float [] floatArr = new float[intArr.length];
        for (int n=0; n<intArr.length; n++) {
            floatArr[n] = (float) intArr[n];
        }
        return floatArr;
    }

    /**
     * converts double array to float array
     * @param doubleArray
     * @return
     */
    public static float[] doubleToFloat(double[] doubleArray){
        //Takes a 1D double array and converts to a 1D float array
        //Nils Gustafsson

        float[] floatArray = new float[doubleArray.length];
        for (int i = 0 ; i < doubleArray.length; i++)
            floatArray[i] = (float) doubleArray[i];
        return floatArray;
    }

    /**
     * converts float array to double array
     * @param floatArray
     * @return
     */
    public static double[] floatToDouble(float[] floatArray){
        //Takes a 1D float array and converts to a 1D double array
        //Nils Gustafsson

        double[] doubleArray = new double[floatArray.length];
        for (int i = 0 ; i < floatArray.length; i++)
            doubleArray[i] = (double) floatArray[i];
        return doubleArray;
    }

    /**
     * converts array list of ints to int array
     * @param integerArray
     * @return
     */
    public static int[] ArrayListInteger2int(ArrayList<Integer> integerArray){
        int[] intArray = new int[integerArray.size()];
        for (int i = 0 ; i < intArray.length; i++)
            intArray[i] = integerArray.get(i);
        return intArray;
    }

    /**
     * converts array list of floats to float array
     * @param FloatArray
     * @return
     */
    public static float[] ArrayListFloat2float(ArrayList<Float> FloatArray){
        float[] floatArray = new float[FloatArray.size()];
        for (int i = 0 ; i < floatArray.length; i++)
            floatArray[i] = FloatArray.get(i);
        return floatArray;
    }
}
