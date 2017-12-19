package nanoj.core.java.aparapi;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.ProfileInfo;
import com.amd.aparapi.Range;
import ij.IJ;
import ij.Prefs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

/**
 * An example Aparapi application which displays a view of the Mandelbrot set and zooms in to a particular point.
 *
 * @author gfrost
 * @author Nils Gustafsson
 * Edited by Nils on 20/03/2015.
 */
public class MandelSample {

    /**
     * An Aparapi Kernel implementation for creating a scaled view of the mandelbrot set.
     *
     * @author gfrost
     * Edited by Nils
     *
     */

    public static class MandelKernel extends Kernel{

        /** RGB buffer used to store the Mandelbrot image. This buffer holds (width * height) RGB values. */
        final private int rgb[];

        /** Mandelbrot image width. */
        final private int width;

        /** Mandelbrot image height. */
        final private int height;

        /** Maximum iterations for Mandelbrot. */
        final private int maxIterations = 64;

        /** Palette which maps iteration values to RGB values. */
        @Constant final private int pallette[] = new int[maxIterations + 1];

        /** Mutable values of scale, offsetx and offsety so that we can modify the zoom level and position of a view. */
        private float scale = .0f;

        private float offsetx = .0f;

        private float offsety = .0f;

        /**
         * Initialize the Kernel.
         *
         * @param _width Mandelbrot image width
         * @param _height Mandelbrot image height
         * @param _rgb Mandelbrot image RGB buffer
         */
        public MandelKernel(int _width, int _height, int[] _rgb) {
            //Initialize palette values
            for (int i = 0; i < maxIterations; i++) {
                final float h = i / (float) maxIterations;
                final float b = 1.0f - (h * h);
                pallette[i] = Color.HSBtoRGB(h, 1f, b);
            }

            width = _width;
            height = _height;
            rgb = _rgb;

        }

        public int getCount(float x, float y) {
            int count = 0;

            float zx = x;
            float zy = y;
            float new_zx = 0f;

            // Iterate until the algorithm converges or until maxIterations are reached.
            while ((count < maxIterations) && (((zx * zx) + (zy * zy)) < 8)) {
                new_zx = ((zx * zx) - (zy * zy)) + x;
                zy = (2 * zx * zy) + y;
                zx = new_zx;
                count++;
            }

            return count;
        }

        @Override public void run() {

            /** Determine which RGB value we are going to process (0..RGB.length). */
            final int gid = getGlobalId();

            /** Translate the gid into an x an y value. */
            final float x = ((((gid % width) * scale) - ((scale / 2) * width)) / width) + offsetx;

            final float y = ((((gid / width) * scale) - ((scale / 2) * height)) / height) + offsety;

            int count = getCount(x, y);

            // Pull the value out of the palette for this iteration count.
            rgb[gid] = pallette[count];
        }

        public void setScaleAndOffset(float _scale, float _offsetx, float _offsety) {
            offsetx = _offsetx;
            offsety = _offsety;
            scale = _scale;
        }

    }

    public static float doMandelbrot() {

        final JFrame frame = new JFrame("MandelBrot");

        /** Width of Mandelbrot view. */
        final int width = 768;

        /** Height of Mandelbrot view. */
        final int height = 768;

        /** Mandelbrot Kernel Range. */
        //final Range range = Range.create(width * height);
        //final Range range = Device.best().createRange(width * height);
        if(CLDevice.chosenDevice == null) {
            CLDevice.setChosenDevice();}
        Range range;
        if (CLDevice.chosenDevice == null || Prefs.get("NJ.kernelMode",false)) {
            range = Range.create(width * height);
        }else{
            range = CLDevice.chosenDevice.createRange(width * height);
        }
        IJ.log("Identity of device used: - " + CLDevicesInfo.deviceIdentity(CLDevice.chosenDevice));
        IJ.log(String.valueOf(range));

        /** Image for Mandelbrot view. */
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final BufferedImage offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Draw Mandelbrot image
        final JComponent viewer = new JComponent(){
            @Override public void paintComponent(Graphics g) {

                g.drawImage(image, 0, 0, width, height, this);
            }
        };

        // Set the size of JComponent which displays Mandelbrot image
        viewer.setPreferredSize(new Dimension(width, height));

        // Swing housework to create the frame
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Extract the underlying RGB buffer from the image.
        // Pass this to the kernel so it operates directly on the RGB buffer of the image
        final int[] rgb = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        final int[] imageRgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // Create a Kernel passing the size, RGB buffer and the palette.
        final MandelKernel kernel = new MandelKernel(width, height, rgb);

        final float defaultScale = 3f;

        // Set the default scale and offset, execute the kernel and force a repaint of the viewer.
        kernel.setScaleAndOffset(defaultScale, -1f, 0f);
        if(Prefs.get("NJ.kernelMode", false)) {
            kernel.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
        }
        kernel.execute(range);

        System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
        viewer.repaint();

        // Report target execution mode: GPU or JTP (Java Thread Pool).
        IJ.log("Execution mode=" + kernel.getExecutionMode());

        float x = -1f;
        float y = 0f;
        float scale = defaultScale;
        final float tox = 0;
        final float toy = 0;

        // This is how many frames we will display as we zoom in and out.
        final int frames = 128;
        final long startMillis = System.currentTimeMillis();
        for (int sign = -1; sign < 2; sign += 2) {
            for (int i = 0; i < (frames - 4); i++) {
                scale = scale + ((sign * defaultScale) / frames);
                x = x - (sign * (tox / frames));
                y = y - (sign * (toy / frames));

                // Set the scale and offset, execute the kernel and force a repaint of the viewer.
                kernel.setScaleAndOffset(scale, x, y);
                kernel.execute(range);
                final List<ProfileInfo> profileInfo = kernel.getProfileInfo();
                if ((profileInfo != null) && (profileInfo.size() > 0)) {
                    for (final ProfileInfo p : profileInfo) {
                        IJ.log(" " + p.getType() + " " + p.getLabel() + " " + (p.getStart() / 1000) + " .. "
                                + (p.getEnd() / 1000) + " " + ((p.getEnd() - p.getStart()) / 1000) + "us");
                    }
                    IJ.log("");
                }

                System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
                viewer.repaint();
            }
        }

        final long elapsedMillis = System.currentTimeMillis() - startMillis;
        //IJ.log("FPS = " + ((frames * 1000) / elapsedMillis));

        kernel.dispose();
        frame.dispose();
        return ((frames * 1000) / elapsedMillis);
    }
}