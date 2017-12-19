package nanoj.core.java.gui;

import com.amd.aparapi.device.Device;
import ij.IJ;
import ij.Prefs;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.aparapi.MandelSample;

import java.io.IOException;

import static nanoj.core.java.aparapi.CLDevicesInfo.deviceIdentity;
import static nanoj.core.java.aparapi.CLDevicesInfo.deviceList;

/**
 * Created by user on 20/03/2015.
 */
public class Mandelbrot_ extends _BaseDialog_{

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
        IJ.log("Running Device Tests");
        String bestDevice = deviceIdentity(Device.best());
        float FPSmax = 0;
        float FPS = 0;
        for (String device : deviceList()) {
            IJ.log(device);
            Prefs.set("NJ.OpenCL.device", device);
            CLDevice.setChosenDevice();
            FPS = MandelSample.doMandelbrot();
            IJ.log("Executed at " + FPS + "FPS");
            if (FPS > FPSmax){

                FPSmax = FPS;
                bestDevice = device;
            }
        }
        Prefs.set("NJ.OpenCL.device", bestDevice);
        IJ.log("Best Device: - " + bestDevice);
        //MandelSample.doMandelbrot();
    }
}
