package nanoj.core.java.optimizers.particleSwarm;

public interface PSOConstants {

    // Velocity generation weighting factors, suggest leaving both as 1
    double Ccog = 1.9; // suggestion from Nobile et al, 2015
    double Csoc = 1.9; // suggestion from Nobile et al, 2015
    // Inertia weighting factors - currently unused
    double W_UP = 0.9; // suggestion from Nobile et al, 2015
    double W_LO = 0.4; // suggestion from Nobile et al, 2015
}
