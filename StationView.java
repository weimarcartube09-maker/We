package com.example.topobt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Esquema cenital sencillo de la estación y su línea de mira. */
public class StationView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public StationView(Context c, AttributeSet a) { super(c, a); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight(), cx=w/2f, cy=h/2f;
        float r=Math.max(28, Math.min(w,h)*.36f);
        p.setColor(0xffeeeeee); p.setStyle(Paint.Style.FILL); c.drawRoundRect(2,2,w-2,h-2,12,12,p);
        p.setColor(0xff9e9e9e); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); c.drawCircle(cx,cy,r,p);
        p.setStyle(Paint.Style.FILL); p.setColor(0xff263238); p.setTextSize(14);
        c.drawText("N", cx-5, cy-r-6, p);
        c.drawLine(cx-r,cy,cx+r,cy,p); c.drawLine(cx,cy-r,cx,cy+r,p);
        double hz=Double.isNaN(Store.lastHz)?0:Store.lastHz;
        double base=Double.isNaN(Store.az0)||Double.isInfinite(Store.az0)?0:Store.az0;
        double a=Math.toRadians(base+hz-90);
        float tx=cx+(float)Math.cos(a)*r*.86f, ty=cy+(float)Math.sin(a)*r*.86f;
        p.setColor(0xff1565c0); p.setStrokeWidth(4); c.drawLine(cx,cy,tx,ty,p);
        float d=10; Path arrow=new Path(); arrow.moveTo(tx,ty); arrow.lineTo(tx-(float)Math.cos(a-.55)*d,ty-(float)Math.sin(a-.55)*d); arrow.lineTo(tx-(float)Math.cos(a+.55)*d,ty-(float)Math.sin(a+.55)*d); arrow.close(); c.drawPath(arrow,p);
        // Dibuja la estación como equipo montado sobre trípode, orientado hacia la mira.
        p.setColor(0xff616161); p.setStrokeWidth(3);
        c.drawLine(cx,cy+5,cx-r*.18f,cy+r*.30f,p);
        c.drawLine(cx,cy+5,cx+r*.18f,cy+r*.30f,p);
        c.drawLine(cx,cy+5,cx,cy+r*.34f,p);
        p.setColor(0xff263238); p.setStyle(Paint.Style.FILL);
        c.save(); c.rotate((float)Math.toDegrees(a)+90,cx,cy);
        c.drawRoundRect(cx-13,cy-8,cx+13,cy+8,4,4,p);
        p.setColor(0xffffb74d); c.drawRect(cx+7,cy-4,cx+20,cy+4,p);
        c.restore();
        p.setColor(0xff111111); p.setTextSize(12); c.drawText("Estación",cx+10,cy-8,p);
        if (!Double.isNaN(Store.lastHz)) { p.setColor(0xff1565c0); c.drawText("Mira",tx+5,ty-4,p); }
    }
}
