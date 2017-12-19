package nanoj.core.java.array;

import java.util.Arrays;

import static java.lang.Math.*;

/**
 *
 * Set of functions for performing array mathematics
 *
 * @author Henriques Lab
 *
 * Created by: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/10/13
 * Time: 12:28 PM
 */
public class ArrayMath {

    /**
     * add elements of two float arrays
     * @param numbers1
     * @param numbers2
     * @return numbers1 + numbers2
     */
    public static float[] add(float[] numbers1, float[] numbers2){
        assert (numbers1.length==numbers2.length);
        float[] numbers = new float[numbers1.length];
        for(int i=0; i < numbers.length;i++)
            numbers[i] = numbers1[i]+numbers2[i];
        return numbers;
    }

    /**
     * subtract elements of two float arrays
     * @param numbers1
     * @param numbers2
     * @return numbers1 - numbers2
     */
    public static float[] subtract(float[] numbers1, float[] numbers2){
        assert (numbers1.length==numbers2.length);
        float[] numbers = new float[numbers1.length];
        for(int i=0; i < numbers.length;i++)
            numbers[i] = numbers1[i]-numbers2[i];
        return numbers;
    }

    /**
     * multiply elements of two float arrays
     * @param numbers1
     * @param numbers2
     * @return numbers1 * numbers2
     */
    public static float[] multiply(float[] numbers1, float[] numbers2){
        assert (numbers1.length==numbers2.length);
        float[] numbers = new float[numbers1.length];
        for(int i=0; i < numbers.length;i++){
            numbers[i] = numbers1[i]*numbers2[i];}
        return numbers;
    }

    /**
     * divide elements of two float arrays
     * @param numbers1
     * @param numbers2
     * @return numbers1 / numbers2 (returns 0 for elements of numbers2 equal to 0)
     */
    public static float[] divideWithZeroDivisionCheck(float[] numbers1, float[] numbers2){
        assert (numbers1.length==numbers2.length);
        float[] numbers = new float[numbers1.length];
        for(int i=0; i < numbers.length;i++){
            if(numbers2[i]==0) {numbers[i] = 0; continue;}
            numbers[i] = numbers1[i]/numbers2[i];}
        return numbers;
    }

    // ---------------- //
    // array statistics //
    // ---------------- //

    /**
     * returns the index of and value of the element containing the max value of a float array
     * @param numbers
     * @return 1d float array with elements [index, value]
     */
    public static float[] getMaxValue(float[] numbers){
        float [] value = new float [2];
        value[0] = 0;
        value[1] = numbers[0];
        for(int i=0; i<numbers.length; i++){
            if(numbers[i] > value[1]){
                value[0] = i;
                value[1] = numbers[i];
            }
        }
        return value;
    }

    /**
     * returns the index of and value of the element containing the max value of a double array
     * @param numbers
     * @return 1d double array with elements [index, value]
     */
    public static double[] getMaxValue(double[] numbers){
        double [] value = new double[2];
        value[0] = 0;
        value[1] = numbers[0];
        for(int i=0; i<numbers.length; i++){
            if(numbers[i] > value[1]){
                value[0] = i;
                value[1] = numbers[i];
            }
        }
        return value;
    }

    /**
     * returns the index of and value of the element containing the min value of a float array
     * @param numbers
     * @return 1d float array with elements [index, value]
     */
    public static float[] getMinValue(float[] numbers){
        float [] value = new float [2];
        value[0] = 0;
        value[1] = numbers[0];
        for(int i=0; i<numbers.length; i++){
            if(numbers[i] < value[1]){
                value[0] = i;
                value[1] = numbers[i];
            }
        }
        return value;
    }

    /**
     * returns the index of and value of the element containing the min value of a double array
     * @param numbers
     * @return 1d double array with elements [index, value]
     */
    public static double[] getMinValue(double[] numbers){
        double [] value = new double[2];
        value[0] = 0;
        value[1] = numbers[0];
        for(int i=0; i<numbers.length; i++){
            if(numbers[i] < value[1]){
                value[0] = i;
                value[1] = numbers[i];
            }
        }
        return value;
    }

    /**
     * returns the index of and value of the element containing the min value (not equal to zero) of a float array
     * @param numbers
     * @return 1d float array with elements [index, value]
     */
    public static float[] getMinNonZeroValue(float[] numbers){
        float [] value = new float [2];
        value[0] = 0;
        value[1] = Float.MAX_VALUE;
        for(int i=0; i<numbers.length; i++){
            if(numbers[i]!=0 && numbers[i] < value[1]){
                value[0] = i;
                value[1] = numbers[i];
            }
        }
        return value;
    }

    /**
     * returns the value of the difference between the max and min values of a float array
     * @param numbers
     * @return max(numbers) - min(numbers)
     */
    public static float getMaxMinusMinValue(float[] numbers){
        float maxV = numbers[0];
        float minV = numbers[0];
        float v;
        for(int i=0; i<numbers.length; i++){
            v = numbers[i];
            maxV = max(v, maxV);
            minV = min(v, minV);
        }
        return maxV-minV;
    }

    /**
     * returns the value of the difference between the max and mean values of a float array
     * @param numbers
     * @return max(numbers) - mean(numbers)
     */
    public static float getMaxMinusMeanValue(float[] numbers){
        float maxV = numbers[0];
        float meanV = 0;
        float v;
        for(int i=0; i<numbers.length; i++){
            v = numbers[i];
            maxV = max(v, maxV);
            meanV += v;
        }
        return maxV-(meanV / numbers.length);
    }

    /**
     * returns the value of the mean of a float array
     * @param numbers
     * @return mean(numbers)
     */
    public static float getAverageValue(float[] numbers){
        double v = 0;
        for(int i=0; i<numbers.length; i++){
            v += numbers[i] / numbers.length;
        }
        return (float) v;
    }

    /**
     * returns the value of the mean of a double array
     * @param numbers
     * @return mean(numbers)
     */
    public static double getAverageValue(double[] numbers){
        double v = 0;
        for(int i=0; i<numbers.length; i++){
            v += numbers[i] / numbers.length;
        }
        return v;
    }

    /**
     * returns the value of the sum of values in a float array
     * @param numbers
     * @return sum(numbers)
     */
    public static double getSumValue(float[] numbers){
        double v = 0;
        for(int i=0; i<numbers.length; i++) v += numbers[i];
        return v;
    }

    /**
     * returns the mean and standard deviation of the smallest X percent of numbers in a float array
     * @param numbers
     * @param percentile
     * @param ignoreZeros
     * @return 2 valued double array [mean(numbers), standarddeviation(numbers)]
     */
    public static double[] getBackgroundMeanAndStdDev(float[] numbers, double percentile, boolean ignoreZeros) {
        float[] numbersSorted = numbers.clone();
        Arrays.sort(numbersSorted);

        // take only lowest %
        int lowestIndex = (int) (numbersSorted.length * percentile);

        int counter = 0;
        double mean = 0;
        double variance = 0;

        for (int i=0; i<numbersSorted.length; i++) {
            if (counter > lowestIndex) break;

            double v = numbersSorted[i];
            if (ignoreZeros && v == 0) continue;

            counter++;
            double oldMean = mean;
            double newMean = oldMean + (v - oldMean) / counter;

            double delta = (v - newMean) * (v - oldMean);
            variance += (delta - variance) / counter;
            mean = newMean;
        }
        double stdDev = sqrt(variance);

        return new double[] {mean, stdDev};
    }

    /**
     * returns the standard deviation of the numbers in a float array
     * @param numbers
     * @return standarddeviation(numbers)
     */
    public static float getStandardDeviationValue(float[] numbers){
        float v = 0;
        float average = getAverageValue(numbers);
        for(int i=0; i<numbers.length; i++){
            v += Math.pow(numbers[i]-average,2);
        }
        v = (float) sqrt(v/numbers.length);
        return v;
    }

    /**
     * returns the mean of the absolute value of the numbers in a float array
     * @param numbers
     * @return mean of abs(numbers)
     */
    public static float getAbsAverageValue(float[] numbers) {
        float v = 0;
        for(int i=0; i<numbers.length; i++){
            v += abs(numbers[i]) / numbers.length;
        }
        return v;
    }

    /**
     * Normalize numbers to range [0, normalizeTo]
     * @param numbers
     * @param normalizeTo
     */
    public static void normalize(float[] numbers, float normalizeTo) {
        if (numbers.length == 1) {
            numbers[0] = normalizeTo;
            return;
        }
        float vMax = numbers[0];
        float vMin = numbers[0];
        for(int i=0; i<numbers.length; i++) {
            vMax = max(numbers[i], vMax);
            vMin = min(numbers[i], vMin);
        }
        if (vMax == 0 && vMin == 0) return;
        vMax -= vMin;
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = (numbers[i]-vMin) * normalizeTo / vMax;
        }
    }

    /**
     * Normalize numbers to range [-1, 1]
     * @param numbers
     */
    public static void normalizeOneMinusOne(float[] numbers) {
        normalize(numbers, 2);
        for(int i=0; i<numbers.length; i++) {
            numbers[i] -= 1;
        }
    }

    /**
     * Normalize numbers to range [min(numbers)*normalizeTo/max(numbers), normalizeTo]
     * @param numbers
     * @param normalizeTo
     */
    public static void normalizeDontSubtractMin(float[] numbers, float normalizeTo) {
        float vMax = getMaxValue(numbers)[1];
        if (vMax == 0) return;
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = numbers[i] * normalizeTo / vMax;
        }
    }

    /**
     * @param numbers
     * @param normalizeTo
     */
    public static void normalizeIntegratedIntensity(float[] numbers, float normalizeTo) {
        float vSum = getAbsAverageValue(numbers) * numbers.length;
        for(int i=0; i<numbers.length; i++) numbers[i] *= normalizeTo / vSum;
    }

    /**
     * @param array1
     * @param array2
     * @param doMeanSubtraction
     * @return
     */
    public static double calculatePPMCC(float[] array1, float[] array2, boolean doMeanSubtraction) {
        if (doMeanSubtraction) {
            array1 = array1.clone();
            array2 = array2.clone();
            float mean;
            mean = getAverageValue(array1);
            for (int n=0; n<array1.length; n++) array1[n] -= mean;
            mean = getAverageValue(array2);
            for (int n=0; n<array2.length; n++) array2[n] -= mean;
        }

        double covariance = 0;
        double squareSum1  = 0;
        double squareSum2  = 0;
        for (int n=0; n<array1.length; n++) {
            float v0 = array1[n];
            float v1 = array2[n];
            covariance += v0*v1;
            squareSum1 += v0*v0;
            squareSum2 += v1*v1;
        }
        double similarity = 0;
        if (squareSum1 !=0 && squareSum2 != 0) similarity = covariance / sqrt(squareSum1 * squareSum2);
        return similarity;
    }

    /**
     * @param array1
     * @param array2
     * @return
     */
    public static double calculateMSE(float[] array1, float[] array2) {
        double MSE = 0;
        int counter = 1;

        for (int n=0; n<array1.length; n++) {
            float v0 = array1[n];
            float v1 = array2[n];
            if (Float.isNaN(v0) || Float.isNaN(v1)) continue;

            MSE += (pow(v0-v1,2)-MSE)/counter;
            counter++;
        }
        return MSE;
    }

    /**
     * Apply generalized Anscombe transform to array
     * @param pixels
     * @param gain
     */
    public static void applyGeneralizedAnscombeTransform(float[] pixels, double gain, double sigma, double offset) {
        double refConstant = 0.375D * gain * gain + sigma * sigma - gain * offset;

        for(int n = 0; n < pixels.length; ++n) {
            double v = (double)pixels[n];
            if (v <= -refConstant / gain) {
                v = 0.0D;
            } else {
                v = 2.0D / gain * Math.sqrt(gain * v + refConstant);
            }
            pixels[n] = (float)v;
        }
    }

    /**
     * Sets any value smaller than v in the array to v
     * @param numbers
     * @param v
     */
    public static void setMinValue(float[] numbers, float v){
        for(int i=0; i < numbers.length;i++){
            if(numbers[i] < v) numbers[i] = v;
        }
    }

    /**
     * Add v to values in array
     * @param numbers
     * @param v
     */
    public static void addWithReplace(float[] numbers, float v){
        for(int i=0; i < numbers.length;i++) numbers[i] += v;
    }

    /**
     * multiply values in array by v and return new array
     * @param numbers
     * @param v
     * @return
     */
    public static float[] multiply(float[] numbers, float v){
        float[] numbers_ = numbers.clone();
        for(int i=0; i < numbers.length;i++) numbers_[i] *= v;
        return numbers_;
    }


    /**
     * Sum array values
     * @param numbers
     * @return
     */
    public static float sum(float[] numbers) {
        float v = 0;
        for(int i=0; i < numbers.length;i++) v+=numbers[i];
        return v;
    }

    public static double sum(double[] numbers) {
        double v = 0;
        for(int i=0; i < numbers.length;i++) v+=numbers[i];
        return v;
    }

    /**
     * Add v to values in float array and return new array
     * @param numbers
     * @param v
     * @return
     */
    public static float[] add(float[] numbers, float v) {
        float[] numbers_ = numbers.clone();
        for(int i=0; i < numbers.length;i++) numbers_[i] += v;
        return numbers_;
    }

    /**
     * Add v to values in int array and return new float array
     * @param numbers
     * @param v
     * @return
     */
    public static float[] add(int[] numbers, float v) {
        float[] numbers_ = new float[numbers.length];
        for(int i=0; i < numbers.length;i++) numbers_[i] = numbers[i]+v;
        return numbers_;
    }

    /**
     * Add value to values in array
     * @param numbers
     * @param value
     */
    public static void add(float[] numbers, double value) {
        for(int i=0; i < numbers.length;i++) numbers[i] += value;
    }

    public static float[] l2norm(float[] numbers1, float[] numbers2){
        assert (numbers1.length==numbers2.length);
        float[] numbers = new float[numbers1.length];
        for(int i=0; i < numbers.length;i++)
            numbers[i] = (float) Math.sqrt(numbers1[i]*numbers1[i]+numbers2[i]*numbers2[i]);
        return numbers;
    }

    ///////////////////////
    // Filter arrays //
    ///////////////////////

    /**
     * @param numbers
     * @param radius
     * @return
     */
    public static float[] movingAverage(float[] numbers, int radius){
        float[] arrayAverage = new float[numbers.length];

        for (int n=0;n<numbers.length;n++) {
            double mean = 0;
            int counter = 1;

            for (int i=-radius;i<=radius;i++){
                double v = numbers[Math.min(max(n + i, 0), numbers.length-1)];
                double oldMean = mean;
                mean = oldMean + (v - oldMean) / counter;
                counter++;
            }

            arrayAverage[n] = (float) mean;
        }
        return arrayAverage;
    }

    /**
     * @param numbers
     * @param radius
     * @return
     */
    public static float[][] movingAverageAndStdDev(float[] numbers, int radius){
        float[] arrayAverage = new float[numbers.length];
        float[] arrayStdDev = new float[numbers.length];

        for (int n=0;n<numbers.length;n++) {

            double mean = 0;
            double variance = 0;
            int counter = 1;

            for (int i=-radius;i<=radius;i++){
                double v = numbers[Math.min(max(n + i, 0), numbers.length-1)];
                mean += (v - mean) / counter;
                counter++;
            }

            counter = 1;
            for (int i=-radius;i<=radius;i++){
                double v = numbers[Math.min(max(n + i, 0), numbers.length-1)];
                double delta = (v - mean) * (v - mean);
                variance += (delta - variance) / counter;
                counter++;
            }

            arrayAverage[n] = (float) mean;
            arrayStdDev[n] = (float) sqrt(variance);
        }
        return new float[][] {arrayAverage, arrayStdDev};
    }
}
