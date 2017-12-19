package nanoj.core.java.gui;

import com.amd.aparapi.device.Device;
import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import nanoj.core.java.aparapi.CLDevice;
import nanoj.core.java.aparapi.CLDevicesInfo;
import nanoj.core.java.tools.Log;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 13/12/14
 * Time: 17:59
 */
public class DisplayOpenCLProperties_ extends _BaseDialog_ {

    Log log = new Log();

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
        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Aparapi OpenCL info...");
        // If Possible Show some Aparapi-OpenCL info
        if(CLDevicesInfo.aparapiFileSearch()) {
            if (Device.best() == null) {
                log.msg(2, "Aparapi Jar Found:- " + CLDevicesInfo.isAparapiJarFound());
                log.msg(2, "Aparapi Lib Found:- " + CLDevicesInfo.isAparapiLibFound());
                log.msg(2, "Multiple Aparapi Files Found:- " + CLDevicesInfo.isMultipleCopiesFound());
                log.msg(2, "Aparapi Jar Location:- " + CLDevicesInfo.getAparapiJarFile().get(0).getAbsolutePath());
                log.msg(2, "Aparapi Lib Location:- " + CLDevicesInfo.getAparapiLibFile().get(0).getAbsolutePath());
                gd.addMessage("Either your computer is not OpenCL compatible or you're missing an Aparapi library." +
                        "\nSee the software website for more info.");
            } else {
                CLDevice.setChosenDevice();
                gd.addMessage("Analysis will be run in:\n" + CLDevice.chosenDevice.toString());

                log.msg(4, CLDevicesInfo.allInfo());

                log.msg(3, "Device Identity List");
                for (String device : CLDevicesInfo.deviceList()) {
                    log.msg(3, device);
                }

                log.msg(3, "Currently Selected Device Identity");
                log.msg(3, Prefs.get("NJ.OpenCL.device", "Auto determined best"));

                String[] properties = System.getProperties().toString().split(",");
                for (int i = 0; i < properties.length; i++) {
                    log.msg(5, properties[i]);
                }

                log.msg(6, "Aparapi Jar Found:- " + CLDevicesInfo.isAparapiJarFound());
                log.msg(6, "Aparapi Lib Found:- " + CLDevicesInfo.isAparapiLibFound());
                log.msg(6, "Multiple Aparapi Files Found:- " + CLDevicesInfo.isMultipleCopiesFound());
                log.msg(6, "Aparapi Jar Location:- " + CLDevicesInfo.getAparapiJarFile().get(0).getAbsolutePath());
                //log.msg(6, "Aparapi Lib Location:- " + CLDevicesInfo.getAparapiLibFile().get(0).getAbsolutePath());

            }
        }else {
            gd.addMessage("Not able to find Aparapi." +
                    "\nSee the software website for more info.");
            String[] properties = System.getProperties().toString().split(",");
            for (int i = 0; i < properties.length; i++) {
                log.msg(1, properties[i]);
            }

            log.msg(1, "Aparapi Jar Found:- " + CLDevicesInfo.isAparapiJarFound());
            log.msg(1, "Aparapi Lib Found:- " + CLDevicesInfo.isAparapiLibFound());
            log.msg(1, "Multiple Aparapi Files Found:- " + CLDevicesInfo.isMultipleCopiesFound());
            log.msg(1, "Aparapi Jar Location:- " + CLDevicesInfo.getAparapiJarFile().get(0).getAbsolutePath());
            log.msg(1, "Aparapi Lib Location:- " + CLDevicesInfo.getAparapiLibFile().get(0).getAbsolutePath());
        }

        // Show dialog
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
    }
}
