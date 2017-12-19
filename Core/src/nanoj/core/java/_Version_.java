package nanoj.core.java;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 10/10/15
 * Time: 11:48
 */
public abstract class _Version_ {

    protected final static String tagInPrefs = "NJ.version";
    protected final static int major = 1;
    protected final static int minor = 0;
    protected final static int status = 0; // 0 - alpha, 1 - beta, 2 - release candidate, 3 - stable
    protected final static int release = 1;
    protected final static String codename = "";
    public final static String header = "NanoJ: ";
    public final static String WHATS_NEW = "";

    public static String headlessGetVersion() {
        String text = major+"."+minor+getStatus()+release;
        if (codename!=null && !codename.equals(""))
            text += " \""+codename+"\"";
        return text;
    }

    public static String getVersion() {
        return header+headlessGetVersion();
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
