package nanoj.core.java;

import ij.Prefs;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 12/12/14
 * Time: 16:33
 */

public class Version extends _Version_ {

    // NanoJ nanoj.core.java.Version
    protected final static String tagInPrefs = "NJ.core.version";
    protected final static int major = 1;
    protected final static int minor = 10;
    protected final static int status = 3; // 0 - alpha, 1 - beta, 2 - release candidate, 3 - stable
    protected final static int release = 1;
    protected final static String codename = "";
    public final static String header = "NanoJ: ";
    public final static String WHATS_NEW =
            "What's new in NanoJ-Core " + headlessGetVersion() + ":\n" +
                    "- Fixes bug not allowing to run aparapi when graphics card drivers are outdated.";

    public static String getVersion() {
        return header+headlessGetVersion();
    }

    public static String headlessGetVersion() {
        String text = major+"."+minor+getStatus()+release;
        if (codename!=null && !codename.equals(""))
            text += " \""+codename+"\"";
        return text;
    }

    public static String headlessGetVersionSmall() {
        return  major+"."+minor;
    }

    public static boolean isNewVersion() {
        if (!headlessGetVersion().equals(Prefs.get(tagInPrefs, ""))) {
            Prefs.set(tagInPrefs, headlessGetVersion());
            Prefs.savePreferences();
            return true;
        }
        return false;
    }

    public static String getStatus(){
        switch (status) {
            case 0: return "Alpha";
            case 1: return "Beta";
            case 2: return "RC";
            case 3: return "Stable";
        }
        return "";
    }
}

