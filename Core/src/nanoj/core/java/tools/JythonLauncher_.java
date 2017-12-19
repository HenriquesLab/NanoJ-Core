package nanoj.core.java.tools;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 2/13/13
 * Time: 11:23 AM
 */

import ij.IJ;
import ij.plugin.PlugIn;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * Responsible for running jython scripts embedded within the jar file. Based on the Fiji JythonLauncher but with
 * some NanoJ tweaks.
 */

public class JythonLauncher_ implements PlugIn {

    PythonInterpreter py = null;
    String path = null;
    String plugPath = null;
    String pyVersion = null;
    boolean error = false;

    /**
     *
     * @param arg the relative path to the script. E.g.: "/qp2jython/Dialogs/Dialog_DetectParticles.py"
     */
    public void run(String arg) {

        try
        {
            // if jython Engine is not started, create it
            if (py == null)
            {
                IJ.showStatus("Warming up NanoJ-Jython Engine...");
                ClassLoader classLoader = IJ.getClassLoader();
                if (classLoader == null)
                    classLoader = getClass().getClassLoader();
                PySystemState.initialize(System.getProperties(), System.getProperties(), new String[] { }, classLoader);
                PySystemState pystate = new PySystemState();
                pystate.setClassLoader(classLoader);
                py = new PythonInterpreter(new PyDictionary(), pystate);

                py.exec("import sys, os, platform, glob");

                // Set plugin path
                /*
                IJ.log(getClass().getResource("\\").toString());
                path = getClass().getResource("/").toString();
                //ij.IJ.log(path);
                py.set("path", path);
                py.exec("path.replace('file:', '')");
                if (ij.IJ.isWindows())
                    py.exec("path.replace('/', os.path.sep)");
                py.exec("path = os.path.abspath(path)");
                py.exec("if path.count('!'): path=path.replace('!', '')");
                py.exec("if path.count(':'): path=path.split(':')[-1]");
                py.exec("sys.path.append(path)");
                py.exec("sys.path.append(os.path.join(path, 'Lib'))");
                */

                // Set NanoJ paths - IJ plugins
                plugPath = IJ.getDirectory("plugins");
                py.set("plugPath", plugPath);
                //ij.IJ.log(plugPath);
                //py.exec("plugPath='"+plugPath+"'");
                if (IJ.isWindows())
                    py.exec("plugPath.replace('/', os.path.sep)");
                py.exec("nanojPlugPath = glob.glob(os.path.join(plugPath, '*NanoJ*.jar'))");
                py.exec("for p in nanojPlugPath: sys.path.append(p)");
                py.exec("for p in nanojPlugPath: sys.path.append(os.path.join(p, 'Lib'))");

                // Set NanoJ paths - MM plugins
                py.set("mmPlugPath", plugPath);
                //ij.IJ.log(plugPath);
                //py.exec("plugPath='"+plugPath+"'");
                if (IJ.isWindows())
                    py.exec("mmPlugPath.replace('/', os.path.sep)");
                py.exec("mmPlugPath = os.path.split(mmPlugPath)[0]");
                py.exec("mmPlugPath = os.path.join(mmPlugPath, 'mmplugins')");
                py.exec("nanojMMPlugPath = glob.glob(os.path.join(mmPlugPath, '*NanoJ*.jar'))");
                py.exec("for p in nanojMMPlugPath: sys.path.append(p)");
                py.exec("for p in nanojMMPlugPath: sys.path.append(os.path.join(p, 'Lib'))");

                // Get sys.path and show...
                py.exec("pySysPath = repr(sys.path)");
                String pySysPath = py.get("pySysPath").toString();
                //ij.IJ.log("NanoJ-Jython Engine sys.path="+pySysPath);


                py.exec("pyVersion = str(platform.python_version())");
                pyVersion = py.get("pyVersion").toString();
                IJ.showStatus("Running NanoJ-jython code (jython "+pyVersion+")...");

                py.execfile(getClass().getResourceAsStream(arg));
                IJ.showStatus("");
            }
            // otherwise, execute file
            else
            {
                IJ.showStatus("Running NanoJ-Jython code...");
                py.execfile(getClass().getResourceAsStream(arg));
                IJ.showStatus("");
            }
            error = false;
        }

        catch (PyException e)
        {
            error = true;
            try
            {
                String msg = "-- Error in "+arg+" --\n"+e.toString();
                IJ.error(msg);
                IJ.log(msg);
            }
            catch (Exception e2)
            {
                e.printStackTrace();
            }
        }
    }
}
