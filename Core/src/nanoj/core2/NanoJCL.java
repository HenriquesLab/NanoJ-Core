package nanoj.core2;

import com.jogamp.opencl.CLBuffer;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.nio.FloatBuffer;

public class NanoJCL {

    public static void fillBuffer(CLBuffer<FloatBuffer> clBuffer, float[] data) {
        FloatBuffer buffer = clBuffer.getBuffer();
        for(int n=0; n<data.length; n++) buffer.put(n, data[n]);
    }

    public static void fillBuffer(CLBuffer<FloatBuffer> clBuffer, ImageProcessor ip) {
        FloatBuffer buffer = clBuffer.getBuffer();
        for(int n=0; n<ip.getPixelCount(); n++) buffer.put(n, ip.getf(n));
    }

    public static void grabBuffer(CLBuffer<FloatBuffer> clBuffer, float[] data) {
        FloatBuffer buffer = clBuffer.getBuffer();
        for(int n=0; n<data.length; n++) data[n] = buffer.get(n);
    }

    public static void grabBuffer(CLBuffer<FloatBuffer> clBuffer, FloatProcessor fp) {
        FloatBuffer buffer = clBuffer.getBuffer();
        for(int n=0; n<fp.getPixelCount(); n++) fp.setf(n, buffer.get(n));
    }

    public static void grabBuffer(CLBuffer<FloatBuffer> clBuffer, ImageStack ims, int nFrames) {
        int w = ims.getWidth();
        int h = ims.getHeight();
        int wh = w * h;
        FloatBuffer buffer = clBuffer.getBuffer();
        for (int f=0; f<nFrames; f++) {
            int offset = f * wh;
            float[] data = new float[wh];
            for(int n=0; n<wh; n++) data[n] = buffer.get(n + offset);
            ims.addSlice(new FloatProcessor(w, h, data));
        }
    }

    /**
     * Replaces the first subsequence of the <tt>source</tt> string that matches
     * the literal target string with the specified literal replacement string.
     *
     * @param source source string on which the replacement is made
     * @param target the string to be replaced
     * @param replacement the replacement string
     * @return the resulting string
     */
    public static String replaceFirst(String source, String target, String replacement) {
        int index = source.indexOf(target);
        if (index == -1) {
            return source;
        }

        return source.substring(0, index)
                .concat(replacement)
                .concat(source.substring(index+target.length()));
    }
}
