package nanoj.core2;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;

public class NanoJUsageTracker {

    private JGoogleAnalyticsTracker tracker;

    public NanoJUsageTracker(String appName, String appVersion, String googleAnalyticsTrackingCode) {

        tracker = new JGoogleAnalyticsTracker(appName, appVersion,  googleAnalyticsTrackingCode);
    }

    public void logUsage(String focus) {
        FocusPoint focusPoint = new FocusPoint(focus);
        tracker.trackAsynchronously(focusPoint);
    }

}
