package nanoj.core.java.gui.tools.projections;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 24/03/15
 * Time: 15:02
 */
public class Projections2D_ extends nanoj.core.java.gui._BaseDialog_ {

    private String filePath;
    private String[] projectionModeChoice = new String[] {"Sum", "Average", "Variance", "Standard Deviation", "Maximum"};

    private int projectionMode, framesPerGroup;
    private boolean useSlidingWindow;
    static String prefsHeader = "NJ.Projections.Projections2D.";

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = false;
        return true;
    }

    public void setupDialog(){
        gd = new NonBlockingGenericDialog("Projections 2D...");
        gd.addChoice("Mode", projectionModeChoice, projectionModeChoice[(int) prefs.get(prefsHeader+"projectionMode", 1)]);
        gd.addCheckbox("Use sliding temporal window", prefs.get(prefsHeader+"useSlidingWindow", false));
        gd.addSlider("Frames per group", 1, imp.getNSlices(), imp.getNSlices());
    }

    public boolean loadSettings() {
        projectionMode = gd.getNextChoiceIndex();
        prefs.set(prefsHeader + "projectionMode", projectionMode);
        useSlidingWindow = gd.getNextBoolean();
        prefs.set(prefsHeader + "useSlidingWindow", useSlidingWindow);
        framesPerGroup = (int) gd.getNextNumber();
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        ImageStack ims = null;

        if (projectionMode == 0)      ims = nanoj.core.java.projections.Projections2D.do2DProjection(imp.getImageStack(), framesPerGroup, useSlidingWindow, nanoj.core.java.projections.Projections2D.SUM);
        else if (projectionMode == 1) ims = nanoj.core.java.projections.Projections2D.do2DProjection(imp.getImageStack(), framesPerGroup, useSlidingWindow, nanoj.core.java.projections.Projections2D.AVERAGE);
        else if (projectionMode == 2) ims = nanoj.core.java.projections.Projections2D.do2DProjection(imp.getImageStack(), framesPerGroup, useSlidingWindow, nanoj.core.java.projections.Projections2D.VARIANCE);
        else if (projectionMode == 3) ims = nanoj.core.java.projections.Projections2D.do2DProjection(imp.getImageStack(), framesPerGroup, useSlidingWindow, nanoj.core.java.projections.Projections2D.STDDEV);
        else if (projectionMode == 4) ims = nanoj.core.java.projections.Projections2D.do2DProjection(imp.getImageStack(), framesPerGroup, useSlidingWindow, nanoj.core.java.projections.Projections2D.MAX);

        if (ims != null) new ImagePlus(projectionModeChoice[projectionMode]+" Projection", ims).show();
    }
}
