package nanoj.core.java.aparapi;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import ij.Prefs;
import nanoj.core.java.tools.Log;

/**
 * NanoJ Kernel extending the aparapi functionality to include choice of device, execution mode and execution on data split by NanoJ blocks
 *
 * @author Ricardo Henriques
 * @author Nils Gustafsson
 *
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 26/03/15
 * Time: 15:54
 */
public class NJKernel extends Kernel {
    public static int NANOJ_MAX_BLOCK_SIZE = 1000000;
    public static EXECUTION_MODE NANOJ_EXECUTION_Mode = null;
    public boolean showProgress = false;
    public String progressPrefix = "";
    private boolean _runningOnceAsJTP = false;
    private EXECUTION_MODE _previousExecutionMode;
    private Log log = new Log();
    protected int blockOffset = 0;

    /**
     * Performs single run of Kernel using Java Thread Pool
     */
    public void runOnceAsJTP(){
        _runningOnceAsJTP = true;
        _previousExecutionMode = getExecutionMode();
    }

    /**
     * Sets execution mode determined by NanoJ preferences
     */
    public void autoChooseDeviceForNanoJ() {

        //if(CLDevice.chosenDevice == null) {setChosenDevice();}
        CLDevice.setChosenDevice();

        if (NANOJ_EXECUTION_Mode != null) {
            setExecutionMode(NANOJ_EXECUTION_Mode);
        }
        else if(Prefs.get("NJ.kernelMode", false)) {
            setExecutionMode(EXECUTION_MODE.JTP);
        }
        else if(_runningOnceAsJTP) {
            setExecutionMode(EXECUTION_MODE.JTP);
            _runningOnceAsJTP = false;
        }
        else if (_previousExecutionMode!=null) {
            setExecutionMode(_previousExecutionMode);
            _previousExecutionMode = null;
        }
    }

    /**
     * Executes the NanoJ Kernel
     * @param rangeSize number of threads to launch
     * @return  executed threads
     */
    @Override
    public Kernel execute(int rangeSize) {
        Range range;
        if (CLDevice.chosenDevice == null || Prefs.get("NJ.kernelMode", false)) {
            range = Range.create(rangeSize);
        }else{
            try {
                range = CLDevice.chosenDevice.createRange(rangeSize);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                setExecutionMode(EXECUTION_MODE.JTP);
                range = Range.create(rangeSize);
            }
        }
        return super.execute(range);
    }

    /**
     * executes the NanoJ kernel in blocks for use when rangeSize exceeds device capabilities
     * @param rangeSize number of threads to launch
     */
    public void executeByBlocks(int rangeSize) {

        int groups = rangeSize / NANOJ_MAX_BLOCK_SIZE;
        if (rangeSize % NANOJ_MAX_BLOCK_SIZE != 0) groups++;

        for (int pG = 0; pG<groups; pG++) {
            if (showProgress) log.msg(progressPrefix+"processing group "+pG+"/"+groups);
            blockOffset = pG * NANOJ_MAX_BLOCK_SIZE;
            int size = min((pG + 1) * NANOJ_MAX_BLOCK_SIZE, rangeSize) - blockOffset;
            execute(size);
        }
    }

    /**
     * run kernel threads
     */
    @Override
    public void run() {

    }
}
