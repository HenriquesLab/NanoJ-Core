package nanoj.core.java.array;

/**
 *
 * Calculate index conversions to/from 1d to higher dimensions
 *
 * @author Henriques Lab
 *
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 27/11/2013
 * Time: 15:38
 */
public class MultidimensionalArrayIndexConversion {

    /**
     * Convert 2d index to 1d index
     * @param x index
     * @param y index
     * @param xs size of 2d array in x
     * @param ys size of 2d array in y
     * @return
     */
    public static int convert2Dto1D(int x, int y, int xs, int ys) {
        return x + y*xs;
    }

    /**
     * Convert 3d index to 1d index
     * @param x index
     * @param y index
     * @param z index
     * @param xs size of 3d array in x
     * @param ys size of 3d array in y
     * @param zs size of 3d array in z
     * @return
     */
    public static int convert3Dto1D(int x, int y, int z, int xs, int ys, int zs) {
        return x + y*xs + z*xs*ys;
    }

    /**
     * Convert 4d index to 1d index
     * @param x index
     * @param y index
     * @param z index
     * @param t index
     * @param xs size of 4d array in x
     * @param ys size of 4d array in y
     * @param zs size of 4d array in z
     * @param ts size of 4d array in t
     * @return
     */
    public static int convert4Dto1D(int x, int y, int z, int t, int xs, int ys, int zs, int ts) {
        return x + y*xs + z*xs*ys + t*xs*ys*zs;
    }

    /**
     * Convert 1d index to 2d index
     * @param p index
     * @param xs size of 2d array in x
     * @param ys size of 2d array in y
     * @return
     */
    public static int[] convert1Dto2D(int p, int xs, int ys) {
        int x = p % xs;
        int y = (p / xs);
        return new int[] {x, y};
    }

    /**
     * Convert 1d index to 3d index
     * @param p index
     * @param xs size of 3d array in x
     * @param ys size of 3d array in y
     * @param zs size of 3d array in z
     * @return
     */
    public static int[] convert1Dto3D(int p, int xs, int ys, int zs) {
        int x = p % xs;
        int y = (p / xs) % ys;
        int z = p / (xs * ys);
        return new int[] {x, y, z};
    }

    /**
     * Convert 1d index to 4d index
     * @param p index
     * @param xs size of 4d array in x
     * @param ys size of 4d array in y
     * @param zs size of 4d array in z
     * @return
     */
    public static int[] convert1Dto4D(int p, int xs, int ys, int zs) {
        int x = p%xs;
        int y = (p/xs)%ys;
        int z = p/(xs*ys)%zs;
        int t = p/(xs*ys*zs);
        return new int[] {x,y,z,t};
    }
}

