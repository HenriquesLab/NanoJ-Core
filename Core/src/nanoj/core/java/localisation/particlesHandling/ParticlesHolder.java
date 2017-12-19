package nanoj.core.java.localisation.particlesHandling;

import ij.IJ;
import ij.Prefs;
import ij.process.FloatPolygon;
import nanoj.core.java.array.ArrayMath;
import nanoj.core.java.array.ArrayTypeConversion;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.*;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 3/26/13
 * Time: 5:03 PM
 */

public class ParticlesHolder{
    public Map<String, String>  metadata;
    public int npoints = 0;
    boolean sortedByTime = false;
    private Map<String, float[]> particleDataArrays;
    private String [] defaultArrayNames = {"signal", "x", "y", "z", "t", "error"};

    public ParticlesHolder(){
        initializeBuffer(100);
    }

    public ParticlesHolder(int nParticlesBuffer){
        if (nParticlesBuffer < 10) nParticlesBuffer = 10;
        initializeBuffer(nParticlesBuffer);
    }

    public ParticlesHolder(Map<String, String> metadata, Map<String, float[]> particleDataArrays, int npoints){
        this.metadata = metadata;
        this.particleDataArrays = particleDataArrays;
        this.npoints = npoints;
    }

    private void initializeBuffer(int nParticlesBuffer){
        particleDataArrays = new HashMap<String, float[]>();
        for (int n = 0; n < defaultArrayNames.length; n++)
        {
            particleDataArrays.put(defaultArrayNames[n], new float[nParticlesBuffer]);
        }
        metadata = new HashMap<String, String>();
    }

//    public ParticlesHolder(Map<String, float[]> particleDataArrays, int npoints){
//        this.metadata = new HashMap<String, String>();
//        this.particleDataArrays = particleDataArrays;
//        this.npoints = npoints;
//    }

    private Map<String, String> duplicateMetadata(){
        Map<String, String>  newMetadata = new HashMap<String, String>();
        String info;
        for (String key : metadata.keySet()) {
            info = metadata.get(key);
            newMetadata.put(key, new String(info));
        }
        return newMetadata;
    }

    public synchronized ParticlesHolder duplicate(){
        // duplicate data arrays
        Map<String, float[]> newParticleDataArrays = new HashMap<String, float[]>();
        float [] original, temp;
        for (String key : particleDataArrays.keySet()) {
            original = particleDataArrays.get(key);
            temp = new float[this.npoints];
            System.arraycopy(original, 0, temp, 0, this.npoints);
            newParticleDataArrays.put(key, temp);
        }
        // duplicate metadata
        Map<String, String> newMetadata = duplicateMetadata();

        return new ParticlesHolder(newMetadata, newParticleDataArrays, this.npoints);
    }

    public synchronized void loadPrefsIntoMetadata() {
        Prefs prefs = new Prefs();
        prefs.savePreferences();
        String prefsPath = prefs.getPrefsDir()+prefs.getFileSeparator()+"IJ_Prefs.txt";
        String txt = IJ.openAsString(prefsPath);
        String[] values;

        for (String line: txt.split("\n")) {
            if (line.startsWith(".NJ.QP2.")){
                line = line.replaceFirst(".NJ.QP2.", "");
                values = line.split("=");
                metadata.put("Settings-"+values[0], values[1]);
            }
        }
    }

    public synchronized ParticlesHolder duplicateForTimePoints(int t) {
        return duplicateForTimePoints(t, t);
    }

    public synchronized ParticlesHolder duplicateForTimePoints(int tStart, int tStop) {

        // grab index of particles in a specific timepoint
        float[] tpoints = particleDataArrays.get("t");
        List<Integer> tIndex = new ArrayList<Integer>();
        for (int n=0; n<npoints; n++)
        {
            //System.out.println(tpoints[n]);
            if (tpoints[n]>=tStart && tpoints[n]<=tStop) {
                tIndex.add(n);
            }
            if (sortedByTime && tpoints[n]>tStop) break;
        }

        // copy particles in timepoint to a new map of particleDataArrays
        Map<String, float[]> newParticleDataArrays = new HashMap<String, float[]>();
        Iterator<Integer> tIndexIterator;
        int nParticles = tIndex.size();
        float [] original, temp;
        int p, counter;

        //System.out.println("nParticles="+nParticles);

        for (String key : particleDataArrays.keySet()) {

            original = particleDataArrays.get(key);
            temp = new float[nParticles];
            counter = 0;
            tIndexIterator = tIndex.iterator();

            while (tIndexIterator.hasNext()) {
                p = tIndexIterator.next();
                temp[counter] = original[p];
                counter ++;;
            }

            newParticleDataArrays.put(key, temp);
        }

        // copy metadata
        Map<String, String> newMetadata = duplicateMetadata();
        return new ParticlesHolder(newMetadata, newParticleDataArrays, nParticles);
    }

    private void expandBufferIfNeeded(){

        if (npoints < particleDataArrays.get("x").length-1)
            return;

        float [] original, temp;
        int newSize = (int) (npoints*1.5);
        for (String key : particleDataArrays.keySet()) {
            original = particleDataArrays.get(key);
            temp = new float[newSize];
            System.arraycopy(original, 0, temp, 0, npoints);

            particleDataArrays.put(key, temp);
        }
    }

    private int getBufferSize(){
        return particleDataArrays.get("x").length;
    }

    public synchronized void appendNewDataArray(String name, float[] values){
        assert (npoints == values.length);
        if (values.length == particleDataArrays.get("x").length)
        {
            particleDataArrays.put(name, values);
        }
        else
        {
            float[] newValues = new float[particleDataArrays.get("x").length];
            System.arraycopy(values, 0, newValues, 0, npoints);
            particleDataArrays.put(name, newValues);
        }
    }

    public synchronized float[] appendNewDataArray(String key){
        if (particleDataArrays.containsKey(key)) return particleDataArrays.get(key);
        float [] values = new float[particleDataArrays.get("x").length];
        particleDataArrays.put(key, values);
        return values;
    }

    public synchronized void addMetaInfo(String key, String value){
        metadata.put(key, value);
    }

//    public synchronized String[] getMetaKeys(){
//        String[] keys = new String[metadata.size()];
//        int counter = 0;
//        for (String key : metadata.keySet()) {
//            keys[counter] = key;
//        }
//        return keys;
//    }
//
//    public synchronized String getMetaInfo(String key){
//        return metadata.get(key);
//    }
//
//    public synchronized String getMetaDict(String key){
//        return metadata.get(key);
//    }

    public String[] getDataArrayNames(){
        String [] names = new String[particleDataArrays.size()];
        int counter = 0;

        for (String key: defaultArrayNames) {
            names[counter] = key;
            counter++;
        }
        // in case the user has added extra arrays aside from the default ones
        for (String key: particleDataArrays.keySet()) {
            if (!ArrayUtils.contains(defaultArrayNames, key))
            names[counter] = key;
            counter++;
        }
        return names;
    }

    /**
     * Returns a pointer or copy of the corresponding internal data array
     * @param key name of the internal data array to return
     * @param truncatedToNPoints if true then return a copy of the data array truncated to npoints, otherwise return a
     *                           pointer to the internal data array
     * @return pointer or copy of the corresponding internal data array
     */
    public float[] getDataArray(String key, boolean truncatedToNPoints) {
        float [] arrayOriginal = particleDataArrays.get(key);
        if (!truncatedToNPoints || arrayOriginal.length==npoints) {
            return arrayOriginal;
        }
        float[] array = new float[npoints];
        System.arraycopy(arrayOriginal, 0, array, 0, this.npoints);
        return array;
    }

    /**
     * Same a calling getDataArray with truncatedToNPoints as true
     * @param key
     * @return
     */
    public float[] getDataArray(String key) {
        return getDataArray(key, true);
    }

    public synchronized void addPoint(String[] keys, float [] values){
        assert(keys.length == values.length);
        assert(keys.length <= particleDataArrays.size());

        expandBufferIfNeeded();

        for (int n=0; n<keys.length; n++) {
            if (!particleDataArrays.containsKey(keys[n])) appendNewDataArray(keys[n]);
            particleDataArrays.get(keys[n])[npoints] = values[n];
        }

        if (keys.length < particleDataArrays.size()) { // fill any missing values with 0s
            for (String key: particleDataArrays.keySet()) {
                if (!ArrayUtils.contains(keys, key)) {
                    particleDataArrays.get(key)[npoints] = 0;
                }
            }
        }
        npoints++;
    }

    public synchronized FloatPolygon toFloatPolygon(){
        float[] xpoints = particleDataArrays.get("x");
        float[] ypoints = particleDataArrays.get("y");
        return new FloatPolygon(xpoints, ypoints, npoints);
    }

    public synchronized int getMinT(){
        return (int) getMinDataArrayValue("t");
    }

    public synchronized int getMaxT(){
        return (int) getMaxDataArrayValue("t");
    }

    public synchronized float getMinDataArrayValue(String key) {return ArrayMath.getMinValue(particleDataArrays.get(key))[1];}

    public synchronized float getMaxDataArrayValue(String key) {return ArrayMath.getMaxValue(particleDataArrays.get(key))[1];}

    public synchronized void incrementT(int t){
        float[] time = particleDataArrays.get("t");
        for (int n=0; n<npoints; n++)
        {
            time[n] += t;
        }
    }

    public synchronized float[] getMagnifiedArray(String key, float magnification) {
        float[] arr = particleDataArrays.get(key);
        float[] newArray = new float[npoints];
        for (int n=0; n<npoints; n++)
            newArray[n] = arr[n]*magnification;
        return newArray;
    }

    public synchronized void appendPolygon(Polygon plg, int time, float xoffset, float yoffset) {
        FloatPolygon fplg = new FloatPolygon(
                ArrayTypeConversion.intArray2floatArray(plg.xpoints),
                ArrayTypeConversion.intArray2floatArray(plg.ypoints),
                plg.npoints);
        appendPolygon(fplg, time, xoffset, yoffset);
    }

    public synchronized void appendPolygon(FloatPolygon fplg, int time, float xoffset, float yoffset) {

        String [] entrySequence = {"signal", "x", "y", "z", "t"};
        float [] entryValues = new float[entrySequence.length];

        entryValues[0] = 1; // Always 1
        entryValues[4] = time;

        for (int n = 0; n<fplg.npoints; n++)
        {
            entryValues[1] = fplg.xpoints[n]+xoffset;
            entryValues[2] = fplg.ypoints[n]+yoffset;
            this.addPoint(entrySequence, entryValues);
        }
    }

    public synchronized void appendParticlesHolder(ParticlesHolder peaks) {

        int newNPoints = this.npoints+peaks.npoints;
        float [] arr1, arr2, temp;

        boolean directCopy = false;
        if (getBufferSize() > newNPoints)
            directCopy = true;

        for (String key : peaks.particleDataArrays.keySet()) {

            arr1 = particleDataArrays.get(key);
            arr2 = peaks.particleDataArrays.get(key);

            if (directCopy)
                System.arraycopy(arr2, 0, arr1, this.npoints, peaks.npoints);
            else {
                temp = new float[(int) floor(newNPoints*1.5)+1];
                System.arraycopy(arr1, 0, temp, 0, this.npoints);
                System.arraycopy(arr2, 0, temp, this.npoints, peaks.npoints);
                particleDataArrays.put(key, temp);
            }
        }
        this.npoints = newNPoints;
    }

    /**
     * @return (closest index, lateral distance, axial distance, temporal distance)
     */
    public synchronized float [] getClosest(float x, float y, float z, float t) {
        int min_p = -1;
        double lateral_distance = 999999999;
        double axial_distance = 999999999;
        double temporal_distance = 999999999;
        double _lateral_distance, _axial_distance, _temporal_distance;
        float x0, y0, z0, t0;

        float[] xpoints = particleDataArrays.get("x");
        float[] ypoints = particleDataArrays.get("y");
        float[] zpoints = particleDataArrays.get("z");
        float[] tpoints = particleDataArrays.get("t");

        for (int i=0; i<npoints; i++)
        {
            x0 = xpoints[i];
            y0 = ypoints[i];
            z0 = zpoints[i];
            t0 = tpoints[i];

            _temporal_distance = abs(t0-t);
            if (_temporal_distance > temporal_distance) continue;

            _lateral_distance = sqrt(pow(x0-x, 2)+pow(y0-y, 2));
            _axial_distance = abs(z0-z);

            if (_lateral_distance < lateral_distance) {
                min_p = i;
                lateral_distance = _lateral_distance;
                axial_distance = _axial_distance;
                temporal_distance = _temporal_distance;
            }
        }

        if (min_p==-1)
        {
            return null;
        }

        float [] values = new float[4];
        values[0] = min_p;
        values[1] = (float) lateral_distance;
        values[2] = (float) axial_distance;
        values[3] = (float) temporal_distance;
        return values;
    }

    /**
     * Calculations based on http://bigwww.epfl.ch/smlm/challenge/index.html?p=tools
     * @param trueParticles
     * @param toleranceLateralRadius
     * @param toleranceAxialRadius
     * @param toleranceTemporalRadius
     * @return (rmsd_lateral, rmsd_axial, rmsd_temporal, rmsd_signal,
     *          detection recall, detection precision, detection f-score, detection Jaccard index)
     */
    public synchronized Map<String, Float> getDetectionError(ParticlesHolder trueParticles,
                                      float toleranceLateralRadius,
                                      float toleranceAxialRadius,
                                      float toleranceTemporalRadius) {

        ParticlesHolder measuredParticles = this;

        float x, y, z, t, s, lateral_distance, axial_distance, temporal_distance, signal_distance;
        float [] values;

        int tp = 0; // true positive
        int fp = 0; // false positive
        int fn = 0; // false negative
        float rmsd_lateral = 0; // root-mean square distance
        float rmsd_axial = 0; // root-mean square distance
        float rmsd_temporal = 0; // root-mean square distance
        float rmsd_signal = 0; // root-mean square distance
        float xBias = 0;
        float yBias = 0;
        int p;

        float [] trueXPoints = trueParticles.getDataArray("x", true);
        float [] trueYPoints = trueParticles.getDataArray("y", true);
        float [] trueZPoints = trueParticles.getDataArray("z", true);
        float [] trueTPoints = trueParticles.getDataArray("t", true);
        float [] trueSPoints = trueParticles.getDataArray("signal", true);

        float [] measuredXPoints = getDataArray("x", true);
        float [] measuredYPoints = getDataArray("y", true);
        float [] measuredSPoints = getDataArray("signal", true);

        for (int i=0; i<trueParticles.npoints; i++)
        {
            x = trueXPoints[i];
            y = trueYPoints[i];
            z = trueZPoints[i];
            t = trueTPoints[i];
            s = trueSPoints[i];

            values = getClosest(x, y, z, t);
            if (values == null) {
                fn++;
                continue;
            }

            p = (int) values[0];
            lateral_distance = values[1];
            axial_distance = values[2];
            temporal_distance = values[3];
            signal_distance = abs(s-measuredSPoints[p]);

            //System.out.println(lateral_distance);

            if (lateral_distance <= toleranceLateralRadius &&
                    axial_distance <= toleranceAxialRadius &&
                    temporal_distance <= toleranceTemporalRadius) {
                xBias           += x-measuredXPoints[p];
                yBias           += y-measuredYPoints[p];
                rmsd_lateral    += (lateral_distance * lateral_distance);
                rmsd_axial      += (axial_distance * axial_distance);
                rmsd_temporal   += (temporal_distance * temporal_distance);
                rmsd_signal     += (signal_distance * signal_distance);
                tp++;
            }
            else fn++;
        }
        fp = npoints - trueParticles.npoints;

        xBias         /= tp;
        yBias         /= tp;
        rmsd_lateral  /= tp;
        rmsd_axial    /= tp;
        rmsd_temporal /= tp;
        rmsd_signal   /= tp;

        float detection_recall = (float) tp / (tp+fn);
        float detection_precision = (float) tp / (tp+fp);
        float detection_f_score = 2*detection_precision*detection_recall / (detection_precision+detection_recall);
        float detection_Jaccard = (float) tp / (npoints + trueParticles.npoints + tp);

        Map<String, Float> results = new HashMap<String, Float>();

        results.put("xBias", xBias);
        results.put("yBias", yBias);

        results.put("RMSD lateral", rmsd_lateral);
        results.put("RMSD axial", rmsd_axial);
        results.put("RMSD temporal", rmsd_temporal);
        results.put("RMSD signal", rmsd_signal);

        results.put("detection recall", detection_recall);
        results.put("detection f-score", detection_f_score);
        results.put("detection Jaccard", detection_Jaccard);

        results.put("detection true-positives", (float) tp);
        results.put("detection false-positives", (float) fp);
        results.put("detection false-negatives", (float) fn);

        return results;
    }

    public synchronized String getDetectionErrorString(ParticlesHolder trueParticles,
                                          float toleranceLateralRadius,
                                          float toleranceAxialRadius,
                                          float toleranceTemporalRadius) {

        Map<String, Float> results = getDetectionError(trueParticles,
                                            toleranceLateralRadius,
                                            toleranceAxialRadius,
                                            toleranceTemporalRadius);
        String txt = "";

        txt += "Bias   X-axis= "+results.get("xBias")+"\n";
        txt += "Bias   Y-axis= "+results.get("yBias")+"\n";
        txt += "RMSD  lateral= "+results.get("RMSD lateral")+"\n";
        if (results.get("RMSD axial")!=0)
            txt += "RMSD    axial= "+results.get("RMSD lateral")+"\n";
        if (results.get("RMSD temporal")!=0)
            txt += "RMSD temporal= "+results.get("RMSD temporal")+"\n";
        txt += "RMSD   signal= "+results.get("RMSD signal")+"\n";
        txt += "Detection        recall= "+results.get("detection recall")+"\n";
        txt += "Detection     precision= "+results.get("detection precision")+"\n";
        txt += "Detection       f-score= "+results.get("detection f-score")+"\n";
        txt += "Detection Jaccard index= "+results.get("detection Jaccard")+"\n";

        txt += "Particles true-positive= "+results.get("detection true-positives")+"\n";
        txt += "Particles false-positive= "+results.get("detection false-positives")+"\n";
        txt += "Particles false-negative= "+results.get("detection false-negatives")+"\n";

        txt += "Peaks="+npoints+"/"+trueParticles.npoints;
        return txt;
    }
}

