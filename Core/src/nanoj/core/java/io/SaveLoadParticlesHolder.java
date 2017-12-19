package nanoj.core.java.io;

import ij.IJ;
import ij.Prefs;
import nanoj.core.java.Version;
import nanoj.core.java.localisation.particlesHandling.ParticlesHolder;
import nanoj.core.java.tools.DateTime;
import org.apache.commons.lang3.StringUtils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 12/12/14
 * Time: 15:34
 */
public class SaveLoadParticlesHolder {

    private String path;
    private Prefs prefs = new Prefs();

    public SaveLoadParticlesHolder(String path){
        this.path = path;
    }

    public void save(ParticlesHolder peaks) throws IOException {
        save(peaks, false);
    }

    /**
     *
     * @param peaks
     * @param append if true, appends to existing particles file
     */
    public void save(ParticlesHolder peaks, boolean append) throws IOException {

        IJ.showStatus("Saving QuickPALM data into file...");

        FileWriter fw = new FileWriter(path, append);
        String [] arrayNames = peaks.getDataArrayNames();

        fw.write("#Creation-date: " + DateTime.getDateTime() + "\n");
        fw.write("#Metadata: QuickPALM-version: " + Version.getVersion() + "\n");

        if (!append) {
            peaks.loadPrefsIntoMetadata();

            // write metadata
            String metadata = "";
            for (String key : peaks.metadata.keySet())
                metadata += "#Metadata: "+key+": "+peaks.metadata.get(key)+"\n";
            fw.write(metadata);

            // write header
            fw.write(StringUtils.join(arrayNames, "\t")+"\n");
        }

        // write data
        String data = "";
        String[] lineData;
        int counter;
        float[][] arrayData = new float[arrayNames.length][];
        // - grab the array data first
        counter = 0;
        for (String key : arrayNames) {
            arrayData[counter] = peaks.getDataArray(key, false);
            counter++;
        }
        // - write data into string
        IJ.showProgress(0);
        for (int n=0;n<peaks.npoints;n++)
        {
            if (n%1000==0) IJ.showProgress(n, peaks.npoints-1); // only update progress every 1000 lines

            lineData = new String[arrayNames.length];
            for (int arrIndex=0; arrIndex<arrayNames.length; arrIndex++) {
                lineData[arrIndex] = ""+arrayData[arrIndex][n];
            }
            fw.write(StringUtils.join(lineData, "\t")+"\n");
        }
        fw.close();
        IJ.showProgress(1);

        IJ.showStatus("Done...");
    }

    public ParticlesHolder load() throws IOException {

        IJ.showStatus("Loading QuickPALM data into file...");

        String txt = IJ.openAsString(path);
        String[] lines = txt.split("\n");

        ParticlesHolder peaks = new ParticlesHolder(lines.length);
        String line;
        String [] entrySequence = null;
        float [] entryValues;
        boolean firstComment = true;

        for (int n=0; n<lines.length; n++){
            if (n%1000==0) IJ.showProgress(n, lines.length-1); // only update progress every 1000 lines

            line = lines[n];

            if (line.startsWith("#")){ // metadata or comment
                if (line.startsWith("#Metadata: ")) {
                    String [] values = line.split(": ");
                    assert (values.length == 3);
                    peaks.metadata.put(values[1], values[2]);
                }
                else {
                    if (firstComment) {
                        IJ.log("Ignoring comment: ");
                        firstComment = false;
                    }
                    IJ.log(line);
                }
            }
            else {
                if (entrySequence == null) { // we should be getting the header line
                    entrySequence = line.split("\t");
                }
                else {
                    String[] values = line.split("\t");
                    assert (values.length == entrySequence.length);
                    entryValues = new float[entrySequence.length];
                    for (int v=0; v<values.length; v++) {
                        entryValues[v] = Float.parseFloat(values[v]);
                    }
                    peaks.addPoint(entrySequence, entryValues);
                }
            }
        }
        IJ.showProgress(1);
        IJ.showStatus("Done...");
        return peaks;
    }

    private void loadPrefs(){
        prefs.savePreferences();
        String prefsPath = prefs.getPrefsDir()+prefs.getFileSeparator()+"IJ_Prefs.txt";
        String txt = IJ.openAsString(prefsPath);

        for (String line: txt.split("\n")) {
            if (line.startsWith(".NJ.QP2.")){
                line = line.replaceFirst(".NJ.QP2.", "");

            }
        }
    }
}
