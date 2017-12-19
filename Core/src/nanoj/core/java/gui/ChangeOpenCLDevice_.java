package nanoj.core.java.gui;

import com.amd.aparapi.device.Device;
import ij.IJ;
import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.aparapi.CLDevicesInfo;
import nanoj.core.java.tools.Log;

/**
 * Created by user on 18/03/2015.
 */
public class ChangeOpenCLDevice_ implements PlugIn {

    private Log log = new Log();

    public void run(String arg){

        String currentDevice = Prefs.get("NJ.OpenCL.device","Auto determined best");

        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Change OpenCL Device");
        // Show some Aparapi-OpenCL info
        if (Device.best() == null)
        {
            gd.addMessage("Either your computer is not OpenCL compatible or you're missing an Aparapi library." +
                    "\nSee the software website for more info.");

            // Show dialog
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
        }
        else
        {
            
            gd.addMessage("Current Device:-\t" + currentDevice);

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

            // Show dialog
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }

            String deviceChoice = gd.getNextRadioButton();
            IJ.log("Radio Button choice: - " + deviceChoice);
            Prefs.set("NJ.OpenCL.device",deviceChoice);
            Prefs.savePreferences();
            CLDevice.setChosenDevice();
        }
    }
}
