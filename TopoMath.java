package com.example.topobt;

import java.util.Locale;

/** Programas de topografía. Los puntos se escriben y se muestran como E (este), N (norte), Z (cota). */
public class TopoMath {

    public static boolean gms = false; // true: los ángulos se escriben como GG.MMSS
    public static boolean sectionReverse = false;
    public static String sectionElement = "Recta";
    public static String sectionPlateName = "Placa objetivo";

    public interface Run { String run(double[] v); }

    public static class Prog {
        public final String name;
        public final String[] in;   // "P:" punto (N,E,Z) | "A:" ángulo | "N:" número
        public final Run run;
        Prog(String name, String[] in, Run run) { this.name = name; this.in = in; this.run = run; }
    }

    public static final Prog[] PROGS = {
        new Prog("Línea base 2 puntos",
            new String[]{"P:Punto A", "P:Punto B"}, v -> base(v, false)),
        new Prog("Línea base 2 puntos con cotas",
            new String[]{"P:Punto A", "P:Punto B"}, v -> base(v, true)),
        new Prog("Azimut y distancia → punto",
            new String[]{"P:Punto base", "A:Azimut", "N:Distancia horizontal"}, TopoMath::radiar),
        new Prog("Arco 3 puntos",
            new String[]{"P:Punto 1 (PC)", "P:Punto 2", "P:Punto 3 (PT)"}, v -> arco3(v, false)),
        new Prog("Arco 3 puntos con cotas",
            new String[]{"P:Punto 1 (PC)", "P:Punto 2", "P:Punto 3 (PT)"}, v -> arco3(v, true)),
        new Prog("Arco: punto + radio",
            new String[]{"P:Punto inicial (PC)", "A:Azimut tangente", "N:Radio", "A:Deflexión Δ (+der / -izq)"},
            TopoMath::arcoRadio),
        new Prog("Curva circular (PI)",
            new String[]{"P:PI", "A:Azimut entrada", "A:Azimut salida", "N:Radio"}, TopoMath::curva),
        new Prog("Espiral - curva - espiral",
            new String[]{"P:PI", "A:Azimut entrada", "A:Azimut salida", "N:Radio", "N:Longitud espiral Ls"},
            TopoMath::espiral),
        new Prog("Área de polígono",
            new String[0], v -> "Ingrese tres o más puntos para calcular el área."),
        new Prog("Sección vial / placa",
            new String[]{"P:Punto medido", "P:Punto eje", "A:Azimut tangente", "N:Kilometraje del eje (m)",
                "N:Alzado de diseño en el eje (m)", "N:Pendiente longitudinal (%)", "N:Desplazamiento de diseño (m)",
                "N:Espesor de capa (m)", "N:Desplazamiento placa lateral (m)", "A:Ángulo incluido",
                "N:Pendiente de placa (%)", "N:Ensanchamiento de placa (m)"}, TopoMath::seccionVial),
    };

    // ---------- utilidades ----------
    /** Convierte la entrada del usuario a grados decimales. */
    public static double ang(double v) {
        if (!gms) return v;
        long x = Math.round(Math.abs(v) * 10000);
        double d = x / 10000 + ((x / 100) % 100) / 60.0 + (x % 100) / 3600.0;
        return v < 0 ? -d : d;
    }
    static double rad(double d) { return Math.toRadians(d); }
    static double norm(double d) { d %= 360; if (d < 0) d += 360; return d; }
    static double azim(double dN, double dE) { return norm(Math.toDegrees(Math.atan2(dE, dN))); }
    static String dms(double d) {
        double a = Math.abs(d); int g = (int) a; double mm = (a - g) * 60; int m = (int) mm;
        double s = (mm - m) * 60;
        if (s >= 59.995) { s = 0; m++; }
        if (m >= 60) { m = 0; g++; }
        return String.format(Locale.US, "%s%d°%02d'%05.2f\"", d < 0 ? "-" : "", g, m, s);
    }
    static String A(double d) { return String.format(Locale.US, "%.4f° = %s", d, dms(d)); }
    static String f(double x) { return String.format(Locale.US, "%.3f", x); }
    static String pt(String n, double N, double E) {
        return String.format(Locale.US, "%s  E=%.3f  N=%.3f\n", n, E, N);
    }
    static String ptz(String n, double N, double E, double Z) {
        return String.format(Locale.US, "%s  E=%.3f  N=%.3f  Z=%.3f\n", n, E, N, Z);
    }
    public static String area(double[][] pts) {
        if (pts == null || pts.length < 3) return "El área requiere al menos 3 puntos.\n";
        int count=pts.length;
        if(count>3&&samePoint(pts[0],pts[count-1]))count--;
        if(count<3)return "El área requiere al menos 3 vértices distintos.\n";
        double twice=0, perimeter=0, perimeter3d=0, area3d=0;
        boolean hasZ=true;
        for(int i=0;i<count;i++)if(pts[i].length<3||Double.isNaN(pts[i][2])||Double.isInfinite(pts[i][2]))hasZ=false;
        for(int i=0;i<count;i++){
            double[] a=pts[i], b=pts[(i+1)%count];
            twice += a[1]*b[0]-b[1]*a[0];
            double dn=b[0]-a[0],de=b[1]-a[1];
            perimeter += Math.hypot(dn,de);
            if(hasZ){double dz=b[2]-a[2];perimeter3d+=Math.sqrt(dn*dn+de*de+dz*dz);}
        }
        double area2d=Math.abs(twice)/2;
        if(hasZ)for(int i=1;i<count-1;i++){
            double[] o=pts[0],a=pts[i],b=pts[i+1];
            double ax=a[1]-o[1],ay=a[0]-o[0],az=a[2]-o[2];
            double bx=b[1]-o[1],by=b[0]-o[0],bz=b[2]-o[2];
            double cx=ay*bz-az*by,cy=az*bx-ax*bz,cz=ax*by-ay*bx;
            area3d+=0.5*Math.sqrt(cx*cx+cy*cy+cz*cz);
        }
        String s=String.format(Locale.US,"Polígono cerrado · %d vértices\nÁrea en planta (2D): %.3f m²\nPerímetro en planta: %.3f m\n",count,area2d,perimeter);
        return hasZ?s+String.format(Locale.US,"Área de superficie (3D): %.3f m²\nPerímetro 3D: %.3f m\n",area3d,perimeter3d):s+"Área 3D: falta la cota Z en uno o más vértices.\n";
    }
    public static boolean samePoint(double[] a,double[] b){return a!=null&&b!=null&&a.length>=2&&b.length>=2&&Math.hypot(a[0]-b[0],a[1]-b[1])<=0.001;}
    public static class SectionResult {
        public double station,measuredOffset,measuredElevation,targetOffset,axisDesignElevation,designElevation,lowerElevation,verticalCorrection,lateralCorrection,sectionAzimuth,plateSlope;
    }
    public static SectionResult sectionResult(double[] v){
        double az=rad(v[6]),dN=v[0]-v[3],dE=v[1]-v[4];
        double along=dN*Math.cos(az)+dE*Math.sin(az);
        double included=rad(v[13]);
        double sectionAz=az+included;
        double lateral=dN*Math.cos(sectionAz)+dE*Math.sin(sectionAz);
        if(sectionReverse){along=-along;lateral=-lateral;}
        SectionResult r=new SectionResult();r.station=v[7]+along;r.measuredOffset=lateral;r.measuredElevation=v[2];
        r.targetOffset=v[10]+v[12]+v[15];r.sectionAzimuth=norm(Math.toDegrees(sectionAz)+(sectionReverse?180:0));
        r.axisDesignElevation=v[8]+along*v[9]/100.0;r.plateSlope=v[14];
        r.designElevation=r.axisDesignElevation+r.targetOffset*v[14]/100.0;
        r.lowerElevation=r.designElevation-v[11];
        r.verticalCorrection=r.lowerElevation-v[2];r.lateralCorrection=r.targetOffset-lateral;return r;
    }
    static String seccionVial(double[] v){SectionResult r=sectionResult(v);String vertical=Math.abs(r.verticalCorrection)<0.001?"A nivel":(r.verticalCorrection>0?"Subir ":"Bajar ")+f(Math.abs(r.verticalCorrection))+" m";
        String lateral=Math.abs(r.lateralCorrection)<0.001?"En posición":(r.lateralCorrection>0?"Mover hacia afuera ":"Mover hacia adentro ")+f(Math.abs(r.lateralCorrection))+" m";
        return String.format(Locale.US,"Sección vial · %s · %s\nKilometraje calculado: K%.0f+%07.3f\nDesplazamiento medido: %.3f m\nDesplazamiento objetivo: %.3f m\nAlzado de diseño: %.3f m\nH inferior (con espesor): %.3f m\n%s\n%s\nÁngulo incluido: %.4f°\nAzimut tangente: %.4f°\nAzimut de sección: %.4f°\nPendiente longitudinal: %.3f %%\nPendiente de placa: %.3f %%\nEspesor de capa: %.3f m\nPlaca: %s · Ensanchamiento: %.3f m\nElemento: %s · Orientación: %s",sectionPlateName,sectionElement,Math.floor(r.station/1000.0),r.station-Math.floor(r.station/1000.0)*1000.0,r.measuredOffset,r.targetOffset,r.designElevation,r.lowerElevation,vertical,lateral,v[13],v[6],r.sectionAzimuth,v[9],v[14],v[11],sectionPlateName,v[15],sectionElement,sectionReverse?"Pequeño kilometraje":"Gran kilometraje");}
    static double[] mov(double N, double E, double az, double d) {
        return new double[]{N + d * Math.cos(rad(az)), E + d * Math.sin(rad(az))};
    }

    // ---------- programas ----------
    static String base(double[] v, boolean z) {
        double dN = v[3] - v[0], dE = v[4] - v[1], d = Math.hypot(dN, dE);
        if (d < 1e-9) return "Los dos puntos son iguales.\n";
        double az = azim(dN, dE);
        String s = "ΔN = " + f(dN) + "   ΔE = " + f(dE) + "\n"
                + "Distancia horizontal: " + f(d) + " m\n"
                + "Azimut A→B: " + A(az) + "\n"
                + "Azimut B→A: " + A(norm(az + 180)) + "\n";
        if (z) {
            if (Double.isNaN(v[2]) || Double.isNaN(v[5])) return s + "Faltan las cotas.\n";
            double dz = v[5] - v[2];
            s += "ΔCota (B-A): " + f(dz) + " m\n"
               + String.format(Locale.US, "Pendiente: %.3f %%\n", 100 * dz / d)
               + "Distancia inclinada: " + f(Math.hypot(d, dz)) + " m\n";
        }
        return s;
    }

    static String radiar(double[] v) {
        double[] p = mov(v[0], v[1], v[3], v[4]);
        return pt("Punto nuevo", p[0], p[1]);
    }

    static String arco3(double[] v, boolean z) {
        double ox = v[1], oy = v[0];
        double x2 = v[4] - ox, y2 = v[3] - oy, x3 = v[7] - ox, y3 = v[6] - oy;
        double dd = 2 * (x2 * y3 - x3 * y2);
        if (Math.abs(dd) < 1e-9) return "Los 3 puntos están alineados: no forman arco.\n";
        double s2 = x2 * x2 + y2 * y2, s3 = x3 * x3 + y3 * y3;
        double cx = (s2 * y3 - s3 * y2) / dd, cy = (s3 * x2 - s2 * x3) / dd;
        double R = Math.hypot(cx, cy);
        double cross = x2 * (y3 - y2) - y2 * (x3 - x2);
        int sg = cross < 0 ? 1 : -1;                       // 1 = derecha
        double a1 = azim(-cy, -cx), a2 = azim(y2 - cy, x2 - cx), a3 = azim(y3 - cy, x3 - cx);
        double delta = sg > 0 ? norm(a3 - a1) : norm(a1 - a3);
        double d12 = sg > 0 ? norm(a2 - a1) : norm(a1 - a2);
        double L = R * rad(delta), C = 2 * R * Math.sin(rad(delta / 2));
        double azIn = norm(a1 + 90 * sg), azOut = norm(a3 + 90 * sg);
        String s = pt("Centro", cy + oy, cx + ox)
                + "Radio R = " + f(R) + " m\n"
                + "Sentido: " + (sg > 0 ? "derecha" : "izquierda") + "\n"
                + "Deflexión Δ = " + A(delta) + "\n"
                + "Longitud de arco L = " + f(L) + " m\n"
                + "Cuerda C = " + f(C) + " m\n"
                + "Azimut tangente entrada: " + A(azIn) + "\n"
                + "Azimut tangente salida: " + A(azOut) + "\n";
        if (delta < 179.9) {
            double T = R * Math.tan(rad(delta / 2));
            double[] pi = mov(v[0], v[1], azIn, T);
            s += "Tangente T = " + f(T) + " m\n"
               + "Externa E = " + f(R * (1 / Math.cos(rad(delta / 2)) - 1)) + " m\n"
               + "Flecha M = " + f(R * (1 - Math.cos(rad(delta / 2)))) + " m\n"
               + pt("PI", pi[0], pi[1]);
        }
        if (z) {
            if (Double.isNaN(v[2]) || Double.isNaN(v[5]) || Double.isNaN(v[8])) return s + "Faltan cotas.\n";
            double pend = (v[8] - v[2]) / L;
            double zi = v[2] + (v[8] - v[2]) * (R * rad(d12)) / L;
            s += String.format(Locale.US, "Pendiente sobre el arco: %.3f %%\n", 100 * pend)
               + "Cota interpolada en P2: " + f(zi) + "\n"
               + "Diferencia con la cota medida en P2: " + f(v[5] - zi) + " m\n";
        }
        return s;
    }

    static String arcoRadio(double[] v) {
        double az = v[3], R = v[4], dl = v[5];
        if (R <= 0 || Math.abs(dl) < 1e-9) return "Radio y deflexión deben ser distintos de cero.\n";
        int sg = dl >= 0 ? 1 : -1;
        double D = Math.abs(dl);
        double[] cc = mov(v[0], v[1], az + 90 * sg, R);
        double[] pt2 = mov(cc[0], cc[1], az - 90 * sg + dl, R);
        double L = R * rad(D), T = R * Math.tan(rad(D / 2));
        String s = pt("Centro", cc[0], cc[1]) + pt("PT", pt2[0], pt2[1])
                + "Azimut tangente salida: " + A(norm(az + dl)) + "\n"
                + "Longitud de arco L = " + f(L) + " m\n"
                + "Cuerda C = " + f(2 * R * Math.sin(rad(D / 2))) + " m\n";
        if (D < 179.9) {
            double[] pi = mov(v[0], v[1], az, T);
            s += "Tangente T = " + f(T) + " m\n" + pt("PI", pi[0], pi[1]);
        }
        return s;
    }

    static double deflexion(double az1, double az2) {
        double d = norm(az2 - az1);
        return d > 180 ? d - 360 : d;
    }

    static String curva(double[] v) {
        double az1 = v[3], az2 = v[4], R = v[5];
        double dl = deflexion(az1, az2);
        if (R <= 0 || Math.abs(dl) < 1e-9) return "Radio positivo y azimutes distintos.\n";
        int sg = dl > 0 ? 1 : -1;
        double D = Math.abs(dl), h = rad(D / 2);
        double T = R * Math.tan(h), L = R * rad(D);
        double[] pc = mov(v[0], v[1], az1, -T), pt2 = mov(v[0], v[1], az2, T);
        double[] cc = mov(pc[0], pc[1], az1 + 90 * sg, R);
        return "Deflexión Δ = " + A(D) + (sg > 0 ? "  (derecha)" : "  (izquierda)") + "\n"
            + "Tangente T = " + f(T) + " m\n"
            + "Longitud L = " + f(L) + " m\n"
            + "Cuerda larga C = " + f(2 * R * Math.sin(h)) + " m\n"
            + "Externa E = " + f(R * (1 / Math.cos(h) - 1)) + " m\n"
            + "Flecha M = " + f(R * (1 - Math.cos(h))) + " m\n"
            + (R >= 10 ? "Grado curvatura (cuerda 20 m): " + A(Math.toDegrees(2 * Math.asin(10 / R))) + "\n" : "")
            + pt("PC", pc[0], pc[1]) + (Double.isNaN(v[2]) ? pt("PI", v[0], v[1]) : ptz("PI", v[0], v[1], v[2])) + pt("PT", pt2[0], pt2[1])
            + pt("Centro", cc[0], cc[1]);
    }

    static String espiral(double[] v) {
        double az1 = v[3], az2 = v[4], R = v[5], Ls = v[6];
        double dl = deflexion(az1, az2);
        if (R <= 0 || Ls <= 0 || Math.abs(dl) < 1e-9) return "Radio y Ls positivos, y azimutes distintos.\n";
        int sg = dl > 0 ? 1 : -1;
        double D = Math.abs(dl), h = rad(D / 2);
        double th = Ls / (2 * R);                                   // θs en radianes
        if (rad(D) < 2 * th) return "Δ es menor que 2θs: aumenta el radio o reduce Ls.\n";
        double Xc = Ls * (1 - th * th / 10 + Math.pow(th, 4) / 216);
        double Yc = Ls * (th / 3 - Math.pow(th, 3) / 42 + Math.pow(th, 5) / 1320);
        double p = Yc - R * (1 - Math.cos(th)), k = Xc - R * Math.sin(th);
        double Ts = (R + p) * Math.tan(h) + k, Es = (R + p) / Math.cos(h) - R;
        double Lc = R * (rad(D) - 2 * th);
        double[] te = mov(v[0], v[1], az1, -Ts), st = mov(v[0], v[1], az2, Ts);
        double[] ec = mov(te[0], te[1], az1, Xc), ec2 = mov(ec[0], ec[1], az1 + 90 * sg, Yc);
        double[] cs = mov(st[0], st[1], az2, -Xc), cs2 = mov(cs[0], cs[1], az2 + 90 * sg, Yc);
        double[] c0 = mov(te[0], te[1], az1, k), cc = mov(c0[0], c0[1], az1 + 90 * sg, R + p);
        return "Deflexión Δ = " + A(D) + (sg > 0 ? "  (derecha)" : "  (izquierda)") + "\n"
            + "θs = " + A(Math.toDegrees(th)) + "\n"
            + "Xc = " + f(Xc) + "   Yc = " + f(Yc) + "\n"
            + "p = " + f(p) + "   k = " + f(k) + "\n"
            + "Tangente total Ts = " + f(Ts) + " m\n"
            + "Externa Es = " + f(Es) + " m\n"
            + "Longitud curva circular Lc = " + f(Lc) + " m\n"
            + "Longitud total = " + f(Lc + 2 * Ls) + " m\n"
            + pt("TE (tangente-espiral)", te[0], te[1])
            + pt("EC (espiral-curva)", ec2[0], ec2[1])
            + pt("CE (curva-espiral)", cs2[0], cs2[1])
            + pt("ET (espiral-tangente)", st[0], st[1])
            + (Double.isNaN(v[2]) ? pt("PI", v[0], v[1]) : ptz("PI", v[0], v[1], v[2])) + pt("Centro", cc[0], cc[1]);
    }
}
