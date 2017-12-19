package nanoj.core.java.io.zip;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.ImageWriter;
import ij.io.TiffEncoder;
import ij.process.ImageProcessor;
import nanoj.core.java.tools.DateTime;
import nanoj.core.java.tools.Prefs;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static nanoj.core.java.Version.headlessGetVersion;

/**
 * Created by paxcalpt on 15/03/15.
 */
public class SaveFileInZip {

    private Prefs prefs = new Prefs();
    private ZipOutputStream zOut = null;
    private FileOutputStream fOut = null;
    private BufferedOutputStream bOut = null;
    private String filePath;
    private String filePathFirstZip = null;
    private boolean useMultipleZips = true;
    private long maxSizeBytes = (long) 1e9;
    private int zipFileCounter = 0;
    private long accumulativeSize = 0;
    private int level = prefs.getCompressionLevel();

    public SaveFileInZip(String filePath, boolean useMultipleZips) throws IOException {
        this.filePath = filePath;
        this.useMultipleZips = useMultipleZips;
        if (useMultipleZips) {
            initializeMultiFileStreams();
        }
        else {
            fOut = new FileOutputStream(filePath);
            bOut = new BufferedOutputStream(fOut, Prefs.STREAM_BUFFER_SIZE);
            zOut = new ZipOutputStream(bOut);
            zOut.setLevel(level);
        }
        zOut.putNextEntry(new ZipEntry("_NanoJInfo_.txt"));
        PrintWriter pw = new PrintWriter(zOut);
        pw.println("#-C:CreationDate:" + DateTime.getDateTime());
        pw.println("#-C:NanoJVersion:" + headlessGetVersion());
        pw.flush();
        zOut.closeEntry();
    }

    public String getFilePathFirstZip() {
        return filePathFirstZip;
    }

    private void setNextZipIfNeeded() throws IOException {
        if (!useMultipleZips) return;
        if (fOut.getChannel().size() < maxSizeBytes) return;
        close();
        initializeMultiFileStreams();
    }

    private void initializeMultiFileStreams() throws FileNotFoundException {
        String filePath;
        if      (this.filePath.endsWith(".njb")) filePath = this.filePath.replace(".njb", String.format("-%03d.njb", zipFileCounter));
        else if (this.filePath.endsWith(".nji")) filePath = this.filePath.replace(".nji", String.format("-%03d.nji", zipFileCounter));
        else if (this.filePath.endsWith(".njt")) filePath = this.filePath.replace(".njt", String.format("-%03d.njt", zipFileCounter));
        else     filePath = this.filePath.replace(".zip", String.format("-%03d.zip", zipFileCounter));
        if (filePathFirstZip == null) filePathFirstZip = filePath;
        zipFileCounter++;
        fOut = new FileOutputStream(filePath);
        bOut = new BufferedOutputStream(fOut, Prefs.STREAM_BUFFER_SIZE);
        zOut = new ZipOutputStream(bOut);
        zOut.setLevel(level);
    }

    public void setLevel(int level) {
        this.level = level;
        zOut.setLevel(this.level);
    }

    public long getSize() throws IOException {
        if (fOut == null) return accumulativeSize;
        return accumulativeSize+fOut.getChannel().size();
    }

    synchronized public void addRawImage(String filePathInZip, ImagePlus imp) throws IOException {
        setNextZipIfNeeded();
        zOut.putNextEntry(new ZipEntry(filePathInZip));
        new ImageWriter(imp.getFileInfo()).write(zOut);
        zOut.closeEntry();
    }

    synchronized public void addTiffImage(String filePathInZip, ImagePlus imp) throws IOException {
        if (!filePathInZip.endsWith(".tif") && !filePathInZip.endsWith(".tiff")) filePathInZip += ".tif";
        setNextZipIfNeeded();
        zOut.putNextEntry(new ZipEntry(filePathInZip));
        TiffEncoder tiffEncoder = new TiffEncoder(imp.getFileInfo());
        tiffEncoder.write(zOut);
        zOut.closeEntry();
    }

    synchronized public void addTiffImage(String filePathInZip, ImageProcessor ip) throws IOException {
        addTiffImage(filePathInZip, new ImagePlus("", ip));
    }

    public static String convertSliceLabelIntoNiceTiffName(ImageStack ims, int n) {
        String imageName = ims.getSliceLabel(n);
        if (imageName != null && imageName != "") {
            imageName = ims.getSliceLabel(n).replace("/", "_");

            if (!imageName.matches(".*\\d+.*")) imageName+=String.format("%06d", n-1); // if string does not contain a number
            if (!imageName.endsWith(".tif") && !imageName.endsWith(".tiff"))
                imageName += ".tif";
        }
        else
            imageName = String.format("image%06d.tif", n-1);
        return imageName;
    }

    synchronized public boolean addText(String filePathInZip, String txt) {
        try {
            setNextZipIfNeeded();
            zOut.putNextEntry(new ZipEntry(filePathInZip));
            zOut.write(txt.getBytes());
            zOut.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    synchronized public void addFile(String filePathInZip, String filePath) throws IOException {
        File f = new File(filePath);
        addFile(filePathInZip, f);
    }

    synchronized public void addFile(String filePathInZip, File file) throws IOException {
        setNextZipIfNeeded();
        FileInputStream fIn = new FileInputStream(file);

        zOut.putNextEntry(new ZipEntry(filePathInZip));
        int length;
        byte[] buffer = new byte[1024];
        while ((length = fIn.read(buffer)) > 0) {
            zOut.write(buffer, 0, length);
        }
        zOut.closeEntry();
    }

    synchronized public void addDirectory(String folderPathInZip) throws IOException {
        assert (!useMultipleZips);
        ZipEntry ze = new ZipEntry(folderPathInZip);
        zOut.putNextEntry(ze);
        zOut.closeEntry();
    }

    synchronized public void flush() {
        try {
            zOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public void close() throws IOException {
        accumulativeSize += fOut.getChannel().size();
        if (zOut != null) {
            zOut.flush();
            zOut.close();
            zOut = null;
        }
    }
}
