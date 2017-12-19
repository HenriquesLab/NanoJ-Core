package nanoj.core.java.aparapi;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;
import ij.IJ;
import ij.Prefs;
import nanoj.updater.java.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Returns hardware info available to aparapi from OpenCL Devices and reports on the availability of aparapi
 *
 * @version 1.6
 * @author Ricardo Henriques
 * @author Nils Gustafsson
 *
 */
public class CLDevicesInfo {


    private static final List<File> aparapiLibPath = new ArrayList<File>();
    private static final List<File> aparapiJarPath = new ArrayList<File>();
    private static boolean aparapiLibFound = false;
    private static boolean aparapiJarFound = false;
    private static boolean multipleCopiesFound = false;
    private static boolean alreadySearched = false;
    public static OpenCLDevice chosenDevice = null;

    /**
     * Is aparapi lib found in ImageJ directory
     *
     * @return  <code>true</code> if aparapi lib found;
     *          <code>flase</code> otherwise
     *
     */
    public static boolean isAparapiLibFound() {
        if (!alreadySearched) aparapiFileSearch();
        return aparapiLibFound;
    }

    /**
     * Is aparapi jar found in ImageJ Plugins directory
     *
     * @return  <code>true</code> if aparapi jar found;
     *          <code>flase</code> otherwise
     *
     */
    public static boolean isAparapiJarFound() {
        if (!alreadySearched) aparapiFileSearch();
        return aparapiJarFound;
    }

    /**
     * Have multiple copies of aparapi jar/lib been found in ImageJ
     *
     * @return  <code>true</code> multiple copies found;
     *          <code>flase</code> otherwise
     *
     */
    public static boolean isMultipleCopiesFound(){
        if (!alreadySearched) aparapiFileSearch();
        return multipleCopiesFound;
    }

    /**
     * Returns locations of aparapi lib in ImageJ
     *
     * @return list of file paths to aparapi lib
     *
     */
    public static List<File> getAparapiLibFile(){
        if (!alreadySearched) aparapiFileSearch();
        return aparapiLibPath;
    }

    /**
     * Returns locations of aparapi Jar in ImageJ
     *
     * @return list of file paths to aparapi Jar
     *
     */
    public static List<File> getAparapiJarFile() {
        if (!alreadySearched) aparapiFileSearch();
        return aparapiJarPath;
    }

    /**
     * Returns a string with info on OpenCL available devices, for example:
     * Best device: Device 16925952
     * type:GPU
     * maxComputeUnits=40
     * maxWorkItemDimensions=3
     * maxWorkItemSizes={512, 512, 512}
     * maxWorkWorkGroupSize=512
     * globalMemSize=1610612736
     * localMemSize=65536
     * First CPU: Device 4294967295
     * type:CPU
     * maxComputeUnits=4
     * maxWorkItemDimensions=3
     * maxWorkItemSizes={1024, 1, 1}
     * maxWorkWorkGroupSize=1024
     * globalMemSize=8589934592
     * localMemSize=32768
     * First GPU: Device 16925952
     * type:GPU
     * maxComputeUnits=40
     * maxWorkItemDimensions=3
     * maxWorkItemSizes={512, 512, 512}
     * maxWorkWorkGroupSize=512
     * globalMemSize=1610612736
     * localMemSize=65536
     *
     * @return String info
     */
    public static String getInfo() {
        String info = "Best device: " + Device.best();
        info += "\nFirst CPU: " + Device.firstCPU();
        info += "\nFirst GPU: " + Device.firstGPU();
        return info;
    }

    /**
     *
     * Gets openCL Device based on device type
     *
     * @param type either "Best", "CPU" or "GPU"
     * @return device
     *
     */
    private static Device string2device(String type) {
        Device device = null;
        if (type.equals("Best")) {
            device = Device.best();
        } else if (type.equals("CPU")) {
            device = Device.firstCPU();
        } else if (type.equals("GPU")) {
            device = Device.firstGPU();
        }
        return device;
    }

    /**
     * Returns value defined by prefix in OpenCL device info string
     * @param info
     * @param prefix
     * @return String Value
     */
    private static float valueOf(String info, String prefix) {
        String txt = info;
        txt = txt.substring(txt.indexOf(prefix) + prefix.length());
        txt = txt.substring(0, txt.indexOf("\n"));
        float f = Float.parseFloat(txt);
        return f;
    }

    /**
     * Gets global memory size of device specified
     * @param type either "Best", "CPU" or "GPU"
     * @return 0 if OpenCL device not found, mem size in bytes otherwise
     */
    public static float getGlobalMemSize(String type) {
        Device device = string2device(type);
        if (device == null) return 0;

        return valueOf(device.toString(), "globalMemSize=");
    }

    /**
     * Gets global memory size of 'Best' device
     *
     * @return 0 if OpenCL device not found, mem size in bytes otherwise
     */
    public static float getGlobalMemSizeBest() {
        return getGlobalMemSize("Best");
    }

    /**
     * Gets global memory size of device specified in preferences
     * @return 0 if OpenCL device not found, mem size in bytes otherwise
     */
    public static float getGlobalMemSizeChosenDevice() {
        boolean log = Prefs.get("NJ.debugLevel", 2)>4;

        //chosenDevice = Device.best();
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
        if(chosenDevice == null) return getGlobalMemSizeBest();
        else return (float) chosenDevice.getGlobalMemSize();
    }


    /**
     * gets number of compute units of specified device
     * @param type either "Best", "CPU" or "GPU"
     * @return 0 if OpenCL not found, max number of compute units otherwise
     */
    public static int getComputeUnits(String type) {
        Device device = string2device(type);
        if (device == null) return 0;

        return (int) valueOf(device.toString(), "maxComputeUnits=");
    }

    /**
     * gets number of compute units of 'Best' device
     * @return 0 if OpenCL not found, max number of compute units otherwise
     */
    public static int getComputeUnits() {
        return getComputeUnits("Best");
    }


    /**
     * Returns a string with info on OpenCL available devices:
     * @return
     */
    public static String allInfo() {

        List<OpenCLPlatform> platforms = (new OpenCLPlatform()).getOpenCLPlatforms();
        String allCLInfo = "Machine contains " + platforms.size() + " OpenCL platforms";
        int platformc = 0;
        for (OpenCLPlatform platform : platforms) {
            allCLInfo = allCLInfo + "\nPlatform " + platformc + "{"+
            "\n   Name    : \"" + platform.getName() + "\""+
            "\n   Vendor  : \"" + platform.getVendor() + "\""+
            "\n   nanoj.core.java.Version : \"" + platform.getVersion() + "\"";
            List<OpenCLDevice> devices = platform.getOpenCLDevices();
            allCLInfo = allCLInfo + "\n   Platform contains " + devices.size() + " OpenCL devices";
            int devicec = 0;
            for (OpenCLDevice device : devices) {
                allCLInfo = allCLInfo + ("\n   Device " + devicec + "{"+
                "\n       Type                  : " + device.getType()+
                "\n       GlobalMemSize         : " + device.getGlobalMemSize()+
                "\n       LocalMemSize          : " + device.getLocalMemSize()+
                "\n       MaxComputeUnits       : " + device.getMaxComputeUnits()+
                "\n       MaxWorkGroupSizes     : " + device.getMaxWorkGroupSize()+
                "\n       MaxWorkItemDimensions : " + device.getMaxWorkItemDimensions()+
                "\n   }");
                devicec++;
            }
            allCLInfo = allCLInfo + "\n}";
            platformc++;
        }

        Device bestDevice = OpenCLDevice.best();
        if (bestDevice == null) {
            allCLInfo = allCLInfo + "\nOpenCLDevice.best() returned null!";
        } else {
            allCLInfo = allCLInfo + "\nOpenCLDevice.best() returned { "
            +"\n   Type                  : " + bestDevice.getType()
            +"\n   GlobalMemSize         : " + ((OpenCLDevice) bestDevice).getGlobalMemSize()
            +"\n   LocalMemSize          : " + ((OpenCLDevice) bestDevice).getLocalMemSize()
            +"\n   MaxComputeUnits       : " + ((OpenCLDevice) bestDevice).getMaxComputeUnits()
            +"\n   MaxWorkGroupSizes     : " + ((OpenCLDevice) bestDevice).getMaxWorkGroupSize()
            +"\n   MaxWorkItemDimensions : " + ((OpenCLDevice) bestDevice).getMaxWorkItemDimensions()
            +"\n}";
        }

        Device firstCPU = OpenCLDevice.firstCPU();
        if (firstCPU == null) {
            allCLInfo = allCLInfo + "\nOpenCLDevice.firstCPU() returned null!";
        } else {
            allCLInfo = allCLInfo + "\nOpenCLDevice.firstCPU() returned { "
            +"\n   Type                  : " + firstCPU.getType()
            +"\n   GlobalMemSize         : " + ((OpenCLDevice) firstCPU).getGlobalMemSize()
            +"\n   LocalMemSize          : " + ((OpenCLDevice) firstCPU).getLocalMemSize()
            +"\n   MaxComputeUnits       : " + ((OpenCLDevice) firstCPU).getMaxComputeUnits()
            +"\n   MaxWorkGroupSizes     : " + ((OpenCLDevice) firstCPU).getMaxWorkGroupSize()
            +"\n   MaxWorkItemDimensions : " + ((OpenCLDevice) firstCPU).getMaxWorkItemDimensions()
            +"\n}";
        }

        Device firstGPU = OpenCLDevice.firstGPU();
        if (firstGPU == null) {
            allCLInfo = allCLInfo + "\nOpenCLDevice.firstGPU() returned null!";
        } else {
            allCLInfo = allCLInfo + "\nOpenCLDevice.firstGPU() returned { "
            +"\n   Type                  : " + firstGPU.getType()
            +"\n   GlobalMemSize         : " + ((OpenCLDevice) firstGPU).getGlobalMemSize()
            +"\n   LocalMemSize          : " + ((OpenCLDevice) firstGPU).getLocalMemSize()
            +"\n   MaxComputeUnits       : " + ((OpenCLDevice) firstGPU).getMaxComputeUnits()
            +"\n   MaxWorkGroupSizes     : " + ((OpenCLDevice) firstGPU).getMaxWorkGroupSize()
            +"\n   MaxWorkItemDimensions : " + ((OpenCLDevice) firstGPU).getMaxWorkItemDimensions()
            +"\n}";
        }
        return allCLInfo;
    }

    /**
     * Returns a list of Strings identifying available openCL devices
     * @return List of devices
     */
    public static List<String> deviceList(){

        List<String> deviceList = new ArrayList<String>();
        List<OpenCLPlatform> platforms = (new OpenCLPlatform()).getOpenCLPlatforms();
        int countDevices = 0;
        for (OpenCLPlatform platform : platforms) {
            List<OpenCLDevice> devices = platform.getOpenCLDevices();
            for (OpenCLDevice device : devices) {

                String deviceIdentity = platform.getVendor() + "_" + device.getType();// + "_id_" + device.getDeviceId();
                deviceList.add(countDevices,deviceIdentity);

                countDevices++;
            }
        }
        return deviceList;
    }

    /**
     * Gets the identity of the currently selected device
     * @param deviceSelected
     * @return string identity of device
     */
    public static String deviceIdentity(Device deviceSelected){
        String bestDeviceIdentity = "";

        if(deviceSelected == null) return "JTP";

        String bestDevice = deviceSelected.toString();

        List<OpenCLPlatform> platforms = (new OpenCLPlatform()).getOpenCLPlatforms();
        for (OpenCLPlatform platform : platforms) {
            List<OpenCLDevice> devices = platform.getOpenCLDevices();
            for (OpenCLDevice device : devices) {

                if(bestDevice.equals(device.toString())) {
                    bestDeviceIdentity = platform.getVendor() + "_" + device.getType();// + "_id_" + device.getDeviceId();
                }

            }
        }
        return bestDeviceIdentity;
    }

    /**
     * perform a search for aparapi dependancies
     * @return  <code>true</code> if files found;
     *          <code>false</code> otherwise
     */
    synchronized public static boolean aparapiFileSearch(){
        alreadySearched = true;
        boolean aparapiFilesFound;

        aparapiLibPath.clear();
        aparapiJarPath.clear();
        multipleCopiesFound = false;
        aparapiLibFound = false;
        aparapiJarFound = false;
        aparapiFilesFound = searchDirForAparapi(Path.ijHomeDirectory,false);
        //Move to plugins Dir
        aparapiFilesFound = searchDirForAparapi(Path.pluginsDirectory,aparapiFilesFound);
        //Move to jars Dir
        if (Path.jarsDirectoryExists)
            aparapiFilesFound = searchDirForAparapi(Path.jarsDirectory,aparapiFilesFound);
        return aparapiFilesFound;
    }

    /**
     * search a directory for aparapi dependancies
     * @param directory
     * @param filesFound
     * @return  <code>true</code> if files found;
     *          <code>false</code> otherwise
     */
    private static boolean searchDirForAparapi(String directory, boolean filesFound){
        File Dir = new File(directory);
        File[] folderContents = Dir.listFiles();
        //Search in directory
        if (folderContents != null) {
            for (File folderContent : folderContents) {
                String f = folderContent.toString();
                if (f.contains("aparapi")){
                    if (f.contains(".dll") || f.contains(".dylib") || f.contains(".so")) {
                        if(aparapiLibFound) multipleCopiesFound = true;
                        aparapiLibFound = true;
                        filesFound = true;
                        aparapiLibPath.add(folderContent);
                    }else if(f.contains(".jar")){
                        if(aparapiJarFound) multipleCopiesFound = true;
                        aparapiJarFound = true;
                        filesFound = true;
                        aparapiJarPath.add(folderContent);
                    }
                }
            }
        }
        return filesFound;
    }

}
