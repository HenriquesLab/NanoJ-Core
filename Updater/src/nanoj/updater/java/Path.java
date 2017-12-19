package nanoj.updater.java;

import ij.IJ;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;


/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 2/14/13
 * Time: 11:13 AM
 */

public class Path {

    public static final String ijHomeDirectory = IJ.getDirectory("startup");
    public static final String pluginsDirectory = IJ.getDirectory("plugins");
    public static final String jarsDirectory = ijHomeDirectory + File.separator + "jars";
    public static final boolean jarsDirectoryExists = new File(jarsDirectory).exists();
    public static final String jarsPath = Path.jarsDirectoryExists? Path.jarsDirectory: Path.pluginsDirectory;
    public static final boolean isFIJI = new File(jarsDirectory + File.separator + "Fiji_Updater.jar").exists();


    public static final String TMP = System.getProperty("java.io.tmpdir");
    public static final String JRE = System.getProperty("java.home");

    public String getJarPath() {
        String jar_path;
        //jar_path = getClass().getResource("/").toString();
        jar_path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        return jar_path;
    }

    public static long getCRC32(File f) {
        return getCRC32(f.getAbsolutePath());
    }

    public static long getCRC32(String filePath) {
        CRC32 crc32 = new CRC32();
        FileInputStream fis = null;
        int cnt;

        try {
            fis = new FileInputStream(new File(filePath));
            while ((cnt = fis.read()) != -1) {
                crc32.update(cnt);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return crc32.getValue();
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    public static File searchImageJForFile(String fileName) {
        File f = null;
        if ((f = searchDirForFile(ijHomeDirectory, fileName)) != null) return f;
        if ((f = searchDirForFile(pluginsDirectory, fileName)) != null) return f;
        if (jarsDirectoryExists && (f = searchDirForFile(jarsDirectory, fileName)) != null) return f;
        return f;
    }

    public static File searchDirForFile(String directory, String fileName) {
        File Dir = new File(directory);
        File[] folderContents = Dir.listFiles();
        //Search in directory
        if (folderContents != null) {
            for (File folderContent : folderContents) {
                //System.out.println(folderContent.getName());
                if (folderContent.getName().equals(fileName))
                    return folderContent;
            }
        }
        return null;
    }

}
