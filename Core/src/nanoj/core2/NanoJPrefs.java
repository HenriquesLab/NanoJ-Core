package nanoj.core2;

import ij.Prefs;

public class NanoJPrefs {

    private static Prefs prefs = new Prefs();
    private final String prefsHeader;

    public NanoJPrefs(String prefsHeader) {
        this.prefsHeader = prefsHeader;
    }

    // helper function for title handling
    public String getDefaultImageCheck(String key, String[] titles){
        String targetImage = get(key, titles[0]);
        for(String s:titles){
            if(s==targetImage) return targetImage;
        }
        return titles[0];
    }

    //helper functions for prefs handling
    public float get(String key, float defaultValue) {
        return (float) prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public boolean get(String key, boolean defaultValue) {
        return prefs.get(prefsHeader+"."+key, defaultValue);
    }

    public String get(String key, String defaultValue) { return prefs.get(prefsHeader+"."+key, defaultValue); }

    public void set(String key, float defaultValue) {
        prefs.set(prefsHeader+"."+key, defaultValue);
    }

    public void set(String key, boolean defaultValue) {
        prefs.set(prefsHeader+"."+key, defaultValue);
    }

    public void set(String key, String defaultValue) {prefs.set(prefsHeader+"."+key, defaultValue); }

    public void save() {
        prefs.savePreferences();
    }
}
