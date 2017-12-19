package nanoj.core.java.io.zip.filesInZipTypes;

import java.io.IOException;

public class TextFileInZip extends _BaseFileInZip_ {

    public TextFileInZip(nanoj.core.java.io.zip.OpenFileWithinZip openFileWithinZip, String filePathInZip) {
        super(openFileWithinZip, filePathInZip);
    }

    public String openText() throws IOException {
        return this.openFileWithinZip.loadText(this.filePathInZip);
    }
}
