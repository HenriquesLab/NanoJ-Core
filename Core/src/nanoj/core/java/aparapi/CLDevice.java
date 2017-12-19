package nanoj.core.java.aparapi;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;
import ij.IJ;
import ij.Prefs;

import java.util.List;

import static nanoj.core.java.aparapi.CLDevicesInfo.deviceIdentity;

/**
 *
 * Extends functionality of OpenCLDevice to allow user choice
 *
 * @version 1.6
 * @author Nils Gustafsson
 *
 */
public class CLDevice extends OpenCLDevice{


    public CLDevice(OpenCLPlatform _platform, long _deviceId, TYPE _type) {
        super(_platform, _deviceId, _type);
    }

    public static Device chosenDevice = null;

    /**
     * Sets chosen device to 'Best' or that saved in Prefs
     */
    public static void setChosenDevice(){
        boolean log = Prefs.get("NJ.debugLevel", 2)>4;
        chosenDevice = Device.best();
        String selectedDeviceIdentity = Prefs.get("NJ.OpenCL.device", "Auto determined best");
        if(log) IJ.log("Identity from Prefs: - " + selectedDeviceIdentity);
        List<OpenCLPlatform> platforms = (new OpenCLPlatform()).getOpenCLPlatforms();
        for (OpenCLPlatform platform : platforms) {
            List<OpenCLDevice> devices = platform.getOpenCLDevices();
            for (OpenCLDevice device : devices) {

                if(selectedDeviceIdentity.equals(deviceIdentity(device))) {
                    if(log) IJ.log("Identity of Chosen Device found");
                    chosenDevice = device;
                }
            }
        }
        if(log) IJ.log("Chosen Device Returned: - " + deviceIdentity(chosenDevice));
    }
}
