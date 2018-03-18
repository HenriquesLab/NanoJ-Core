package nanoj.core2;

import com.jogamp.opencl.CLBuffer;
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
}
