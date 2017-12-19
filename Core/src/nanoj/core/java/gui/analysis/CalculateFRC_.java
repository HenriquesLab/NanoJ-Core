package nanoj.core.java.gui.analysis;

import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.ImageProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.analysis.FRC;

import java.io.IOException;

/**
 * Created by paxcalpt on 07/03/2017.
 */
public class CalculateFRC_ extends _BaseDialog_ {
    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = false;
        return true;
    }

    @Override
    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate FRC...");
        gd.addMessage(
                "Try out FRC-Map instead, it's provides better local resolution estimates.\n" +
                "It's under NanoJ-SRRF>Tools");
    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        ImageStack ims = imp.getImageStack();
        if (imp.getImageStack().getSize()<2) {
            log.error("Requires ImageStack with two frames...");
            return;
        }
        ImageProcessor ip1 = ims.getProcessor(1);
        ImageProcessor ip2 = ims.getProcessor(2);

        FRC myFRC = new FRC();
        double res = myFRC.calculateFireNumber(ip1, ip2, FRC.ThresholdMethod.FIXED_1_OVER_7);
        log.msg("FRC Resolution (frame 1-2)="+res+" pix");
    }
}
