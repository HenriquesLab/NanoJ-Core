package nanoj.core.java.tools;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 27/01/15
 * Time: 23:24
 */
public class Memory {
    private static final long MEGABYTE = 1024L * 1024L;
    private static final long GIGABYTE = 1024L * MEGABYTE;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static long bytesToGigabytes(long bytes) {
        return bytes / GIGABYTE;
    }

    public static long usedMemoryBytes(){
        // Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        // Calculate the used memory
        long memory = runtime.totalMemory() - runtime.freeMemory();
        return memory;
    }

    public static long usedMemoryMegaBytes(){
        return bytesToMegabytes(usedMemoryBytes());
    }

    public static long usedMemoryGigaBytes(){
        return bytesToGigabytes(usedMemoryBytes());
    }
}
