package com.example.topobt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Vista en planta de los vértices del polígono del programa Área. */
public class PolygonView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[][] points=new double[0][];
    private boolean closed=false;

    public PolygonView(Context c, AttributeSet a){super(c,a);init();}
    public PolygonView(Context c){super(c);init();}
    private void init(){setBackgroundColor(0xff252525);}
    public void setPoints(double[][] value,boolean isClosed){points=value==null?new double[0][]:value;closed=isClosed;invalidate();}

    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0xff606060);
        for(int i=1;i<5;i++){float x=w*i/5f,y=h*i/5f;c.drawLine(x,24,x,h-24,p);c.drawLine(28,y,w-12,y,p);}
        p.setStyle(Paint.Style.FILL);p.setTextSize(12);p.setColor(0xffcccccc);c.drawText("N",8,20,p);c.drawText("E",w-20,h-6,p);
        if(points.length==0){p.setTextSize(15);c.drawText("Los vértices aparecerán aquí",24,h/2,p);return;}
        double minE=points[0][1],maxE=minE,minN=points[0][0],maxN=minN;
        for(double[] q:points){minE=Math.min(minE,q[1]);maxE=Math.max(maxE,q[1]);minN=Math.min(minN,q[0]);maxN=Math.max(maxN,q[0]);}
        double spanE=Math.max(maxE-minE,0.05),spanN=Math.max(maxN-minN,0.05),scale=Math.min((w-72)/spanE,(h-72)/spanN);
        float usedW=(float)(spanE*scale),usedH=(float)(spanN*scale),ox=(w-usedW)/2f,oy=(h-usedH)/2f;
        Path path=new Path();
        for(int i=0;i<points.length;i++){double[] q=points[i];float x=ox+(float)((q[1]-minE)*scale),y=h-oy-(float)((q[0]-minN)*scale);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
        if(closed&&points.length>=3){path.close();p.setColor(0x5534a853);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);}
        p.setColor(0xff4caf50);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawPath(path,p);
        if(closed&&points.length>=3){float x0=ox+(float)((points[0][1]-minE)*scale),y0=h-oy-(float)((points[0][0]-minN)*scale);float xn=ox+(float)((points[points.length-1][1]-minE)*scale),yn=h-oy-(float)((points[points.length-1][0]-minN)*scale);c.drawLine(xn,yn,x0,y0,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(0xff69c779);p.setTextSize(12);
        for(int i=0;i<points.length;i++){double[] q=points[i];float x=ox+(float)((q[1]-minE)*scale),y=h-oy-(float)((q[0]-minN)*scale);c.drawCircle(x,y,5,p);c.drawText(""+(i+1),x+7,y-7,p);}
        p.setColor(closed?0xff69c779:0xffdddddd);p.setTextSize(12);c.drawText(closed?"Polígono cerrado":"Polígono abierto",34,h-8,p);
    }
}
