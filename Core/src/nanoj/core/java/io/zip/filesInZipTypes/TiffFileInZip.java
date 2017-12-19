package nanoj.core.java.io.zip.filesInZipTypes;

import ij.ImagePlus;
import nanoj.core.java.io.zip.OpenFileWithinZip;

import java.io.IOException;

public class TiffFileInZip extends _BaseFileInZip_ {

    public TiffFileInZip(OpenFileWithinZip openFileWithinZip, String filePathInZip) {
        super(openFileWithinZip, filePathInZip);
    }

    public ImagePlus openImage() throws IOException {
        return this.openFileWithinZip.loadTiffImage(this.filePathInZip);
    }
}
