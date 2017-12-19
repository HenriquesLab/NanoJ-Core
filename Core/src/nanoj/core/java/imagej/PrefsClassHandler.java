package nanoj.core.java.imagej;

import nanoj.core.java.tools.Prefs;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 20/02/2016
 * Time: 10:50
 */
public class PrefsClassHandler {

    private final String prefsHeader;
    private final Prefs prefs = new Prefs();

    public PrefsClassHandler(Class<?> c) {
        prefsHeader = c.getName();
    }

    public int getPrefs(String key, int defaultValue) {
        return (int) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public float getPrefs(String key, float defaultValue) {
        return (float) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public double getPrefs(String key, double defaultValue) {
        return (double) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public boolean getPrefs(String key, boolean defaultValue) {
        return prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public String getPrefs(String key, String defaultValue) {
        return prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public void setPrefs(String key, int value) {
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, float value) {
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, double value) {
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, boolean value) {
        prefs.set(prefsHeader+"."+key, value);
    }

    public void setPrefs(String key, String value) {
        prefs.set(prefsHeader+"."+key, value);
    }

    public void savePrefs() {
        prefs.savePreferences();
    }
}
