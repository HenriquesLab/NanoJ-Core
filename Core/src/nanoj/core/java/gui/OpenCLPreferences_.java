package nanoj.core.java.gui;

import com.amd.aparapi.device.Device;
import ij.IJ;
import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.aparapi.CLDevicesInfo;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 11/12/15
 * Time: 14:32
 */
public class OpenCLPreferences_ extends _BaseDialog_ {
    Prefs prefs = new Prefs();

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = false;
        useSettingsObserver = false;
        return true;
    }

    @Override
    public void setupDialog() {

    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        String currentDevice = Prefs.get("NJ.OpenCL.device","Auto determined best");

        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("OpenCL preferences...");
        // Show some Aparapi-OpenCL info
        if (Device.best() == null) {
            gd.addMessage("Either your computer is not OpenCL compatible or you're missing an Aparapi library." +
                    "\nSee the software website for more info.");
        }
        else {
            gd.addMessage("Current Device: " + currentDevice);

            gd.addCheckbox("Disable OpenCL",prefs.get("NJ.kernelMode",false));

            IJ.log("Device identity list");
            int countDevices = 0;
            for (String device : CLDevicesInfo.deviceList()) {
                IJ.log(device);
                countDevices++;
            }

            IJ.log("Best Device Identity:\n"+ CLDevicesInfo.deviceIdentity(Device.best()));

            String[] devices = new String[countDevices];
            countDevices = 0;
            for (String device : CLDevicesInfo.deviceList()) {
                devices[countDevices] = device;
                countDevices++;
            }

            gd.addRadioButtonGroup("Choose a device", devices, countDevices, 1, Prefs.get("NJ.OpenCL.device", CLDevicesInfo.deviceIdentity(Device.best())));

        }

        // Show dialog
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        prefs.set("NJ.kernelMode", gd.getNextBoolean());
        Prefs.set("NJ.OpenCL.device", gd.getNextRadioButton());
        Prefs.savePreferences();
        CLDevice.setChosenDevice();

        prefs.savePreferences();
    }
}
