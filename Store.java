package com.example.topobt;

import java.util.ArrayList;
import java.util.List;

/** Datos compartidos entre pantallas: estación y puntos leídos. */
public class Store {
    public static final List<String> names = new ArrayList<>();
    public static final List<double[]> points = new ArrayList<>(); // {N, E, Z}

    // Configuración de la estación
    public static double n0 = Double.NaN, e0 = Double.NaN, z0 = Double.NaN;
    public static double az0 = Double.NaN;   // azimut que corresponde a Hz = 0
    public static double hi = 0;    // altura del instrumento
    public static double ht = 0;    // altura del prisma
    public static boolean reflectorless = false;
    public static double lastSd = Double.NaN, lastHz = Double.NaN, lastVz = Double.NaN;
    public static double lastN = Double.NaN, lastE = Double.NaN, lastZ = Double.NaN;
    public static String lastPointName = "";
    public static String folder = "Levantamiento";
    public static Runnable measureAction;
    public static Runnable saveMeasureAction;
    public interface PointListener { void onPoint(double[] p); }
    public static PointListener pointListener;

    public static String add(double n, double e, double z) {
        String name = "P" + (points.size() + 1);
        names.add(name);
        points.add(new double[]{n, e, z});
        return name;
    }
}
