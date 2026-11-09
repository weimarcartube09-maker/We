package com.example.topobt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Perfil transversal: eje de vía, punto medido y posición objetivo. */
public class SectionView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TopoMath.SectionResult result;
    public SectionView(Context c,AttributeSet a){super(c,a);init();}
    public SectionView(Context c){super(c);init();}
    private void init(){setBackgroundColor(0xff252525);}
    public void setResult(TopoMath.SectionResult value){result=value;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(0xff666666);p.setStrokeWidth(1);p.setStyle(Paint.Style.STROKE);
        for(int i=1;i<9;i++)c.drawLine(w*i/9f,24,w*i/9f,h-26,p);for(int i=1;i<5;i++)c.drawLine(36,h*i/5f,w-12,h*i/5f,p);
        p.setStyle(Paint.Style.FILL);p.setTextSize(12);p.setColor(0xffdddddd);c.drawText("Cota",8,18,p);c.drawText("Desplazamiento lateral",w-150,h-6,p);
        if(result==null){p.setTextSize(14);c.drawText("Ingresa los datos y toca CALCULAR",28,h/2,p);return;}
        double minX=Math.min(0,Math.min(result.measuredOffset,result.targetOffset))-1,maxX=Math.max(0,Math.max(result.measuredOffset,result.targetOffset))+1;
        double designMin=result.axisDesignElevation+minX*result.plateSlope/100.0,designMax=result.axisDesignElevation+maxX*result.plateSlope/100.0;
        double minY=Math.min(result.lowerElevation,Math.min(result.measuredElevation,Math.min(designMin,designMax)))-1,maxY=Math.max(result.lowerElevation,Math.max(result.measuredElevation,Math.max(designMin,designMax)))+1;
        if(maxX-minX<1)maxX=minX+1;if(maxY-minY<1)maxY=minY+1;
        float left=38,right=w-16,top=30,bottom=h-30;
        float x0=left+(float)((0-minX)/(maxX-minX))*(right-left), yAxis=bottom-(float)((result.axisDesignElevation-minY)/(maxY-minY))*(bottom-top);
        float yDesignLeft=bottom-(float)((designMin-minY)/(maxY-minY))*(bottom-top),yDesignRight=bottom-(float)((designMax-minY)/(maxY-minY))*(bottom-top);
        p.setColor(0xffbdbdbd);p.setStrokeWidth(2);c.drawLine(left,yDesignLeft,right,yDesignRight,p);c.drawLine(x0,top,x0,bottom,p);
        float xm=left+(float)((result.measuredOffset-minX)/(maxX-minX))*(right-left),ym=bottom-(float)((result.measuredElevation-minY)/(maxY-minY))*(bottom-top);
        float xt=left+(float)((result.targetOffset-minX)/(maxX-minX))*(right-left),yt=bottom-(float)((result.lowerElevation-minY)/(maxY-minY))*(bottom-top);
        p.setColor(0xff42a5f5);p.setStrokeWidth(3);c.drawLine(xt,yt,xm,ym,p);p.setColor(0xff4caf50);c.drawCircle(xm,ym,7,p);p.setColor(0xffffb74d);c.drawCircle(xt,yt,7,p);
        p.setTextSize(12);p.setColor(0xff4caf50);c.drawText("Medido",xm+8,ym-7,p);p.setColor(0xffffb74d);c.drawText("Objetivo",xt+8,yt+16,p);p.setColor(0xffeeeeee);c.drawText("Eje",x0+5,yAxis-5,p);
    }
}
