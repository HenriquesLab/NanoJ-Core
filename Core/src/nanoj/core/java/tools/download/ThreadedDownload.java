package nanoj.core.java.tools.download;

import nanoj.updater.java.Path;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/04/15
 * Time: 19:17
 */
public class ThreadedDownload {

    private static ArrayList<Download> dList = new ArrayList<Download>();

    public static void threadedDownload(String url) {
        int counter = 0;
        while (counter<dList.size()) {
            if (!dList.get(counter).isAlive()) dList.remove(counter);
            else counter++;
        }

        Download d = new Download();
        d.setup(url);
        d.start();
        dList.add(d);
    }
}

class Download extends Thread {
    String urlPath;

    public void setup(String url) {
        urlPath = url;
    }

    public void run() {
        try {
            InputStream is = null;
            URL url = null;

            url = new URL(urlPath);
            URLConnection content = url.openConnection();
            is = content.getInputStream();


            byte[] content1 = readFully(is);
            String fileName = (new File(url.getFile())).getName();
            if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf("?"));
            File out = new File(Path.TMP, fileName);
            FileOutputStream fos = null;

            fos = new FileOutputStream(out);
            fos.write(content1);
            fos.close();
        }
        catch (Exception e) {
        }
    }

    public static byte[] readFully(InputStream is) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            int c1;
            while((c1 = is.read()) != -1) {
                buf.write(c1);
            }

            is.close();
        } catch (IOException var4) {
        }
        return buf.toByteArray();
    }

}