package nanoj.core.java.image.binary;

import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import nanoj.core.java.tools.Log;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.*;

/**
 * Created by Henriques-lab on 20/11/2016.
 */
public class SkeletonAnalysis {

    public ArrayList<Integer> nodes, endPoints, joints, junctions;
    public ArrayList<Filament> filaments;
    public ByteProcessor ip, nodesMap;
    public ShortProcessor filamentsMap;
    private int w, h;
    public static Random random = new Random();

    public SkeletonAnalysis(ByteProcessor ip) {
        if (!ip.isBinary()) return;

        this.ip = ip;
        this.w = ip.getWidth();
        this.h = ip.getHeight();
        calculate();
    }

    private void calculate() {
        nodes = new ArrayList<Integer>();
        endPoints = new ArrayList<Integer>();
        joints = new ArrayList<Integer>();
        junctions = new ArrayList<Integer>();
        nodesMap = new ByteProcessor(w, h);

        for (int j = 1; j < h - 1; j++) {
            for (int i = 1; i < w - 1; i++) {
                int p = j * w + i;

                byte v = (byte) ip.getPixel(i, j);
                if (v == 0) continue;

                nodes.add(p);
                int count = 0;
                if (ip.getPixel(i - 1, j - 1) != 0) count++;
                if (ip.getPixel(i - 1, j) != 0) count++;
                if (ip.getPixel(i - 1, j + 1) != 0) count++;
                if (ip.getPixel(i, j - 1) != 0) count++;
                if (ip.getPixel(i, j + 1) != 0) count++;
                if (ip.getPixel(i + 1, j - 1) != 0) count++;
                if (ip.getPixel(i + 1, j) != 0) count++;
                if (ip.getPixel(i + 1, j + 1) != 0) count++;

                if (count == 1) endPoints.add(p);
                else if (count == 2) joints.add(p);
                else if (count > 2) junctions.add(p);
                nodesMap.set(p, count);
            }
        }
    }

    public void removeJunctions(int radius) {
        for (int p: junctions) {
            int x0 = p % w;
            int y0 = p / w;

            for (int j=-radius; j<=radius; j++) {
                for (int i=-radius; i<=radius; i++) {
                    if (sqrt(i*i+j*j) > radius) continue;
                    int x = min(max(x0 + i, 0), w-1);
                    int y = min(max(y0 + j, 0), h-1);
                    ip.set(x, y, 0);
                }
            }
        }
        calculate();
    }

    public void calculateFilaments(int minSize) {

        filamentsMap = new ShortProcessor(w, h);
        filaments = new ArrayList<Filament>();

        ArrayList<Integer> nodesPool = (ArrayList<Integer>) nodes.clone();
        for (int endPoint: endPoints) {
            Filament f = new Filament();
            boolean isFilament = f.buildFilament(endPoint, nodesPool, w, h);
            if (isFilament && f.nodes.size() >= minSize) {
                filaments.add(f);
                f.render(filamentsMap, random.nextInt(100));
            }
        }
    }

    public void mergeFilaments() {
        int counter = 0;
        while (counter<filaments.size()) {

            Filament filament0 = filaments.get(counter);

            ArrayList<Filament> mergeCandidates = new ArrayList<Filament>();

            for (int id1 = counter+1; id1<filaments.size(); id1++) {
                Filament filament1 = filaments.get(id1);

                double[] bestMergeCandidate = filament0.isMergeCandidate(filament1, 100);
                if (bestMergeCandidate != null) mergeCandidates.add(filament1);
            }

            if (mergeCandidates.size() == 1) {

            }

            counter++;
        }
    }
}

class Filament {
    private static Log log = new Log();
    public ArrayList<Integer> nodes = new ArrayList<Integer>();
    public int[] tips = new int[2];
    public double[] tipsPropagationAngle = new double[2];
    private int w, h;

    public boolean buildFilament(int endPoint, ArrayList<Integer> nodes, int w, int h) {
        this.w = w;
        this.h = h;

        if (!nodes.contains(endPoint)) return false;

        int p0 = endPoint;
        int x0 = endPoint % w;
        int y0 = endPoint / w;
        nodes.add(p0);

        while (true) {
            boolean foundConnection = false;

            for (int n=0; n<nodes.size(); n++) {
                int p1 = nodes.get(n);
                int x1 = p1 % w;
                int y1 = p1 / w;

                if (abs(x1 - x0) <= 1 && abs(y1 - y0) <= 1) {
                    this.nodes.add(p1);
                    nodes.remove(n);
                    x0 = x1;
                    y0 = y1;
                    foundConnection = true;
                }
            }

            if (!foundConnection) break;
        }

        tips[0] = this.nodes.get(0);
        tips[1] = this.nodes.get(this.nodes.size()-1);

        tipsPropagationAngle[0] = getPropagationAngle(tips[0], 20);
        tipsPropagationAngle[1] = getPropagationAngle(tips[1], 20);
        log.msg("x="+(endPoint % w)+" y="+(endPoint / w)+" "+toDegrees(tipsPropagationAngle[0]));

        return true;
    }

    private double getPropagationAngle(int tip, int tipRadius) {

        // calculating tip Center of Mass for propagation angle
        double xCOM = 0;
        double yCOM = 0;
        int nCOM = 0;
        int xTip = tip % w;
        int yTip = tip / w;

        for (int node: this.nodes) {
            int x = node % w;
            int y = node / w;

            double d = sqrt(pow(x-xTip, 2)+pow(y-yTip, 2));
            if (d<=tipRadius) {
                xCOM += x;
                yCOM += y;
                nCOM++;
            }
        }
        xCOM /= nCOM;
        yCOM /= nCOM;

        // get propagationAngle
        double propagationAngle = atan2(xCOM-xTip, yTip-yCOM);
        if (xCOM == xTip && yCOM != yTip) propagationAngle = PI/2d-signum(yCOM-yTip)*PI/2d;
        else if (xCOM != xTip && yCOM == yTip) propagationAngle = signum(xCOM-xTip)*PI/2d;
        else if (xCOM != xTip && yCOM > yTip) propagationAngle = atan2(xCOM-xTip, yTip-yCOM)+signum(xCOM-xTip)*PI;
        else if (xCOM != xTip && yCOM < yTip) propagationAngle = atan2(xCOM-xTip, yTip-yCOM);
        return propagationAngle;
    }

    public void render(ShortProcessor ip, int id) {
        for (int node: nodes) {
            ip.set(node, id);
        }
    }

    public double[] isMergeCandidate(Filament filament, int maxDistance) {
        int tip0 = 0;
        int tip1 = 0;
        double minD = Double.MAX_VALUE;
        double d;

        // get closest tips
        d = sqrt(pow(this.tips[0] % w - filament.tips[0] % w, 2)+pow(this.tips[0] / w - filament.tips[0] / w, 2));
        if (d < minD) {
            tip0 = this.tips[0];
            tip1 = filament.tips[0];
            minD = d;
        }
        d = sqrt(pow(this.tips[1] % w - filament.tips[1] % w, 2)+pow(this.tips[1] / w - filament.tips[1] / w, 2));
        if (d < minD) {
            tip0 = this.tips[1];
            tip1 = filament.tips[1];
            minD = d;
        }
        d = sqrt(pow(this.tips[0] % w - filament.tips[1] % w, 2)+pow(this.tips[0] / w - filament.tips[1] / w, 2));
        if (d < minD) {
            tip0 = this.tips[0];
            tip1 = filament.tips[1];
            minD = d;
        }

        if (minD < maxDistance) return new double[]{tip0, tip1};

        return null;
    }
}