package nanoj.updater.java;

import com.amd.aparapi.Kernel;
import ij.IJ;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 04/04/15
 * Time: 11:27
 */
public class InstallNanoJDependencies {
    // files to copy to Fiji
    public static final String APARAPI_WIN_32_LIB = "aparapi_x86.dll";
    public static final String APARAPI_WIN_64_LIB = "aparapi_x86_64.dll";
    public static final String APARAPI_LIN_32_LIB = "libaparapi_x86.so";
    public static final String APARAPI_LIN_64_LIB = "libaparapi_x86_64.so";
    public static final String APARAPI_MAC_LIB = "libaparapi_x86_64.dylib";

    private static String aparapiLibName;
    private static boolean aparapiFilesAlreadyAutoSelected = false;
    private static String javaVersion;

    public static boolean installed = false;
    public static String executionMode;

    public InstallNanoJDependencies() {
        if (installed == true) return;
        installed = installAparapi();
        Kernel kernel = new Kernel(){
            @Override public void run(){
            }
        } ;
        kernel.execute(8);
        executionMode = ""+kernel.getExecutionMode();
    }

    public static boolean installAparapi() {
        autoSelectAparapiFiles();

        try {
            String pluginsPath = IJ.getDirectory("plugins");
            LibraryPath.addLibraryPath(pluginsPath);
            if (new File(pluginsPath+aparapiLibName).exists()) {
                System.load(pluginsPath + aparapiLibName);
                IJ.showStatus("NanoJ: loaded OpenCL interface...");
                IJ.log("NanoJ: found aparapi libraries in the plugins folder...");
                return true;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        try {
            NativeTools.loadLibraryFromJar("/"+aparapiLibName);
            IJ.showStatus("NanoJ: loaded OpenCL interface...");
            return true;
        } catch (UnsatisfiedLinkError e) {
            IJ.log("WARNING: couldn't load OpenCL libraries (aparapi can't find dependencies), " +
                    "please make sure you have the latest drivers for your graphics card installed.");
            return false;
        } catch (Exception e) {
            IJ.showStatus("NanoJ: could not load OpenCL interface...");
            e.printStackTrace();
            return false;
        }
    }

    synchronized private static boolean autoSelectAparapiFiles() {
        if (aparapiFilesAlreadyAutoSelected) return true;
        aparapiFilesAlreadyAutoSelected = true;

        javaVersion = System.getProperty("java.version");
        javaVersion = javaVersion.substring(0, javaVersion.lastIndexOf("."));

        if(IJ.isLinux()) {
            if (IJ.is64Bit()) {
                aparapiLibName = APARAPI_LIN_64_LIB;
                return true;
            } else {
                aparapiLibName = APARAPI_LIN_32_LIB;
                return true;
            }
        }
        else if (IJ.isWindows()) {
            if (IJ.is64Bit()) {
                aparapiLibName = APARAPI_WIN_64_LIB;
                return true;
            } else {
                aparapiLibName = APARAPI_WIN_32_LIB;
                return true;
            }
        }
        else if (IJ.isMacOSX()) {
            aparapiLibName = APARAPI_MAC_LIB;
            return true;
        }
        IJ.error("Could not find operative system...");
        return false;
    }
}
