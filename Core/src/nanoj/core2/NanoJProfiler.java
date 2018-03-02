package nanoj.core2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.nanoTime;

public class NanoJProfiler {

    private static Map<String, ArrayList<Double>> recordString = new HashMap<String, ArrayList<Double>>();
    private static Map<Integer, Long> recordInts = new HashMap<Integer, Long>();
    private static int idCounter = 0;

    public NanoJProfiler() {

    }

    synchronized public int startTimer() {
        int id = idCounter;
        idCounter++;
        recordInts.put(id, nanoTime());
        return id;
    }

    synchronized public double endTimer(int id) {
        long startTime = recordInts.remove(id);
        long stopTime = nanoTime();
        return (stopTime-startTime)/1E9;
    }

    synchronized public void recordTime(String id, double time) {
        if (!recordString.containsKey(id)) {
            ArrayList<Double> timeList = new ArrayList<Double>();
            recordString.put(id, timeList);
        }
        recordString.get(id).add(time);
    }

    synchronized public String report() {
        String report = "";

        for (String id: recordString.keySet()) {
            ArrayList<Double> timeList = recordString.get(id);
            int ns = timeList.size();
            double meanTime   = 0;
            double stdDevTime = 0;
            for (int n=0; n<ns; n++) meanTime+=timeList.get(n);
            meanTime /= ns;
            for (int n=0; n<ns; n++) stdDevTime+=pow(timeList.get(n)-meanTime, 2);
            stdDevTime = sqrt(stdDevTime / ns);

            report+="Process \""+id+"\" took: "+getTimeString(meanTime)+
                    " meantime (+/- "+getTimeString(stdDevTime)+") over "+ns+" iterations\n";
        }
        return report;
    }

    public String getTimeString(double timeSeconds) {
        String timeString = "";
        if      (timeSeconds < 1/1e6) timeString = round(timeSeconds * 1e9) + "ns";
        else if (timeSeconds < 1/1e3) timeString = round(timeSeconds * 1e6) + "us";
        else if (timeSeconds < 1) timeString = round(timeSeconds * 1e3) + "ms";
        else timeString = round(timeSeconds) + "s";
        return timeString;
    }
}
