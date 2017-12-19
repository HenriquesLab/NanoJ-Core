package nanoj.core.java.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 30/04/15
 * Time: 14:42
 */
public class Options {

    public final static int NEAREST_NEIGHBOUR = 0;
    public final static int BILINEAR = 1;
    public final static int BICUBIC = 2;

    public final static Map<Integer, String> interpolationMethod;
    static {
        interpolationMethod = new LinkedHashMap<Integer, String>();
        interpolationMethod.put(NEAREST_NEIGHBOUR, "None");
        interpolationMethod.put(BILINEAR, "BiLinear");
        interpolationMethod.put(BICUBIC, "BiCubic");
    }

    public static int EXTENSION_TIFF = 0;
    public static int EXTENSION_ZEISS_CZI = 1;
    public static int EXTENSION_NIKON_ND2 = 2;
    public static int EXTENSION_NANOJ_NJI = 3;
    public static int EXTENSION_OLYMPUS_VSI = 4;

    public final static Map<Integer, String> extensionChoice;
    static {
        extensionChoice = new LinkedHashMap<Integer, String>();
        extensionChoice.put(EXTENSION_TIFF, ".tif");
        extensionChoice.put(EXTENSION_ZEISS_CZI, ".czi");
        extensionChoice.put(EXTENSION_NANOJ_NJI, ".nji");
        extensionChoice.put(EXTENSION_NIKON_ND2, ".nd2");
        extensionChoice.put(EXTENSION_OLYMPUS_VSI, ".vsi");
    }
}
