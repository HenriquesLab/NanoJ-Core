package nanoj.core.java.gui;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.boxysystems.jgoogleanalytics.LoggingAdapter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.FolderOpener;
import ij.plugin.PlugIn;
import nanoj.core.java.Version;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;
import nanoj.core.java.tools.Prefs;
import nanoj.updater.java.InstallNanoJDependencies;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static nanoj.core.java.Version.headlessGetVersionSmall;
import static nanoj.core.java.io.OpenNanoJDataset.openNanoJDataset;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 04/01/15
 * Time: 16:29
 */
public abstract class _BaseDialog_ extends Thread implements PlugIn {

    private final static InstallNanoJDependencies NanoJDependecies = new InstallNanoJDependencies();

    public NonBlockingGenericDialog gd = null;
    protected Font headerFont = new Font("Arial", Font.BOLD, 16);

    public ImagePlus imp = null;
    protected ImagePlus impPreview = null;
    protected boolean showPreview = false;

    private boolean firstRun = true;

    protected boolean useSettingsObserver = false;
    protected boolean autoOpenImp = true;

    public String prefsHeader = null, impPath = null;
    protected Prefs prefs = new Prefs();
    protected RefreshViewObserver RVO = new RefreshViewObserver();
    protected String arg;

    protected Log log = new Log();
    protected JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker("NanoJ-Core", headlessGetVersionSmall(), "UA-61590656-3");
    protected FocusPoint parentFocusPoint;

    protected long typingWaitDelay = 500;

    @Override
    public void run() {
        run("");
    }

    @Override
    public void run(String arg) {
        this.arg = arg;

        prefs.incrementUsage();

        if (!beforeSetupDialog(arg)) return;
        if (!showAndAcceptAnnouncements()) return;

        if (autoOpenImp && imp == null){
            setupImp();
            if(imp == null) return;
        }

        if (!NanoJDependecies.installed) {
            log.warning("Could not load OpenCL Aparapi library...");
        }

        if (CLDevice.chosenDevice == null){
            CLDevice.setChosenDevice();
        }

        setupDialog();

        if (gd != null) {
            // Add listener to dialog
            MyDialogListener dl = new MyDialogListener();
            gd.addDialogListener(dl);

            if (useSettingsObserver) RVO.start();
            // Show dialog
            gd.showDialog();
            if (useSettingsObserver) RVO.quit();

            if (gd.wasCanceled()) {
                return;
            }
        }
        else {
            if (!loadSettings()) return;
        }

        afterDoPreview();
        if (!loadSettingsFromPrefs()) return; // also called within the RVO

        try {
            if (impPreview != null) {
                impPreview.close();
                impPreview = null;
            }
            prefs.startNanoJCommand();
            execute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        prefs.savePreferences();
    }

    abstract public boolean beforeSetupDialog(String arg);

    abstract public void setupDialog();

    abstract public boolean loadSettings();

    public boolean loadSettingsFromPrefs(){
        return true;
    }

    public void doPreview() {}

    public void afterDoPreview(){
        if (impPreview != null)
            impPreview.hide();
    }

    abstract public void execute() throws InterruptedException, IOException;

    protected void setupImp(){
        //get open image processor
        imp = WindowManager.getCurrentImage();

        if (imp == null) {
            openImp();
        }
        if(imp == null) return;

        if (imp.isComposite())
            IJ.log("WARNING: detected composite image. NanoJ is optimized for pure image stacks and may yield errors.");
        if (imp.isHyperStack())
            IJ.log("WARNING: detected hyperstack. NanoJ is optimized for pure image stacks and may yield errors.");
    }

    protected boolean isNewVersion() {
        return Version.isNewVersion();
    }

    protected String getWhatsNew() {
        return Version.WHATS_NEW;
    }

    private boolean showAndAcceptAnnouncements() {
        boolean isNewVersion = isNewVersion();
        boolean isNewUser = prefs.isNewUser();

        tracker.setLoggingAdapter(new SystemOutLogger());
        String trackLabel = getClassName();
        if (trackLabel.contains("."))
            trackLabel = trackLabel.substring(trackLabel.lastIndexOf(".")+1, trackLabel.length());
        track(trackLabel);
        //track("NewCall");

        if (isNewUser) this.track("NewUser");
        if (isNewVersion) this.track("NewVersion");

        if (isNewVersion || prefs.showWhatsNew()) {
            NonBlockingGenericDialog gd_whatsNew = new NonBlockingGenericDialog("What's new...");
            gd_whatsNew.addMessage(getWhatsNew());
            gd_whatsNew.addCheckbox("Don't show again...", false);
            gd_whatsNew.showDialog();
            if (gd_whatsNew.wasCanceled()) return false;
            boolean agreed = gd_whatsNew.getNextBoolean();
            if (agreed) prefs.setShowWhatsNew(false);
        }

        return true;
    }

    protected void openImp(){

        impPath = IJ.getFilePath("Choose data to load...");
        if (impPath==null || impPath.equals("")) return;

        if (impPath.endsWith(".nji") || impPath.endsWith(".njb") || impPath.endsWith(".zip")) {
            imp = openNanoJDataset(impPath);
        }
        else if (impPath.endsWith(".tif")) {
            if (impPath.contains("00")) {
                //open image sequence in directory selected and show
                imp = FolderOpener.open(new File(impPath).getParent());
            }
            else {
                imp = IJ.openImage(impPath);
            }
        }
        else {
            imp = IJ.openImage(impPath);
        }
        if(imp == null) return;
        imp.show();
    }

    class MyDialogListener implements DialogListener {
        @Override
        public boolean dialogItemChanged(GenericDialog gd, AWTEvent awtEvent) {
            log.msg(5, "DialogListener: item changed");
            if (!loadSettings()) return false;
            RVO.needsUpdate = true;
            return true;
        }
    }

    private class RefreshViewObserver extends Thread {

        boolean _stop = false;
        boolean needsUpdate = false;

        int nslice = -1;

        public void start() {
            log.msg(4, "RefreshViewObserver: started");
            super.start();
        }

        public void quit(){
            _stop = true;
        }

        public void run(){
            while (!_stop){

                boolean changedSlice = false;
                // ~~ See if something changed ~~
                if (prefs.get("NJ.prefsChanged", false)) { // if a different dialog changed QP2 settings
                    needsUpdate = true;
                    log.msg(4, "RefreshViewObserver: user changed parameter");
                    prefs.set("NJ.prefsChanged", false);
                }
                else if (imp!=null && imp.getSlice()!=nslice) { // if the user changed the current frame in the image
                    needsUpdate = true;
                    changedSlice = true;
                    log.msg(4, "RefreshViewObserver: user changed selected slice");
                    nslice = imp.getSlice();
                }
                // ~~ Act on change ~~
                if (needsUpdate){ // the user is typing, give the boss time to finish
                    if (firstRun) {
                        try {sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                        firstRun = false;
                    }
                    else if (changedSlice) {
                        log.msg(4, "RefreshViewObserver: giving user 3s before updating");
                        try {sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
                    }
                    else {
                        log.msg(4, "RefreshViewObserver: giving user 0.5s before updating");
                        try {sleep(typingWaitDelay);} catch (InterruptedException e) {e.printStackTrace();}
                    }

                    if (!loadSettingsFromPrefs()) gd.getButtons()[0].setEnabled(false);
                    else gd.getButtons()[0].setEnabled(true);
                    updateView();
                    needsUpdate = false;
                }
                // give it 30ms until reprocessing
                try {sleep(30);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }

        public void updateView() {
            log.msg(4, "RefreshViewObserver: updating view...");
            if (showPreview) {
                String title = gd.getTitle();
                gd.setTitle("(calculating)... "+title);
                doPreview();
                gd.setTitle(title);
            }
        }
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public void setPrefsChanged() {
        prefs.set("NJ.prefsChanged", true);
    }

    public int getPrefs(String key, int defaultValue) {
        if (prefsHeader == null) prefsHeader = getClassName();
        return (int) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public float getPrefs(String key, float defaultValue) {
        if (prefsHeader == null) prefsHeader = getClassName();
        return (float) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public double getPrefs(String key, double defaultValue) {
        if (prefsHeader == null) prefsHeader = getClassName();
        return (double) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public boolean getPrefs(String key, boolean defaultValue) {
        if (prefsHeader == null) prefsHeader = getClassName();
        return prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public String getPrefs(String key, String defaultValue) {
        if (prefsHeader == null) prefsHeader = getClassName();
        return prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public void setPrefs(String key, int value) {
        if (prefsHeader == null) prefsHeader = getClassName();
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, float value) {
        if (prefsHeader == null) prefsHeader = getClassName();
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, double value) {
        if (prefsHeader == null) prefsHeader = getClassName();
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, boolean value) {
        if (prefsHeader == null) prefsHeader = getClassName();
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, String value) {
        if (prefsHeader == null) prefsHeader = getClassName();
        prefs.set(prefsHeader+"."+key, value);
    }

    public void savePrefs() {
        prefs.savePreferences();
    }

    public String getPrefsText() {
        return prefs.getNanoJPrefsText(prefsHeader);
    }

    public Map<String, String> getPrefs() {
        return prefs.getNanoJPrefs(prefsHeader);
    }

    public void savePrefsText(String filePath) {
        prefs.saveNanoJPrefsIntoFile(filePath, prefsHeader);
    }

    protected void track(String value) {
        if (parentFocusPoint==null) parentFocusPoint = new FocusPoint(getClassName());
        FocusPoint focus = new FocusPoint(value);
        focus.setParentTrackPoint(parentFocusPoint);
        tracker.trackAsynchronously(focus);
    }
}

class SystemOutLogger implements LoggingAdapter {
    @Override
    public void logError(String s) {
        //System.out.println("errorMessage=" + s);
    }
    @Override
    public void logMessage(String s) {
        //System.out.println(s);
    }
}