package com.example.topobt;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.util.*;

public class ToolsActivity extends Activity {
    private LinearLayout box,programRoot; private TextView tvRes; private final List<EditText> fields=new ArrayList<>(); private final List<EditText[]> pointRows=new ArrayList<>();
    private final List<Character> kinds=new ArrayList<>(); private TopoMath.Prog cur; private EditText multi;
    private CheckBox cbMulti; private PolygonView areaPlot; private SectionView sectionPlot; private EditText sectionPlateName; private EditText[] activePointRow; private int selectedProgram=0; private boolean areaClosed=false;
    private static final int NUM=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setTitle("Programas de topografía");LinearLayout root=new LinearLayout(this);programRoot=root;root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,8,12,8);root.setBackgroundColor(0xff303030);
        Spinner sp=new Spinner(this);String[] names=new String[TopoMath.PROGS.length];for(int i=0;i<names.length;i++)names[i]=TopoMath.PROGS[i].name;ArrayAdapter<String> programAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,names){@Override public View getView(int position,View convert,android.view.ViewGroup parent){TextView t=(TextView)super.getView(position,convert,parent);t.setTextColor(0xffffffff);return t;}@Override public View getDropDownView(int position,View convert,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(position,convert,parent);t.setTextColor(0xffffffff);t.setBackgroundColor(0xff303030);t.setPadding(20,18,20,18);return t;}};programAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sp.setAdapter(programAdapter);sp.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff303030));root.addView(sp);
        CheckBox cb=new CheckBox(this);cb.setText("Ángulos GG.MMSS (123.4530 = 123°45'30\")");cb.setChecked(TopoMath.gms);cb.setOnCheckedChangeListener((v,c)->TopoMath.gms=c);root.addView(cb);
        cbMulti=new CheckBox(this);cbMulti.setText("Ingresar varios PI consecutivos");root.addView(cbMulti);
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);sv.addView(content,new ScrollView.LayoutParams(-1,-2));root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);content.addView(box);
        tvRes=new TextView(this);tvRes.setTypeface(android.graphics.Typeface.MONOSPACE);tvRes.setTextIsSelectable(true);tvRes.setPadding(0,16,0,16);content.addView(tvRes);
        LinearLayout actions=new LinearLayout(this);Button calc=new Button(this);calc.setText("CALCULAR");calc.setTextSize(14);calc.setOnClickListener(v->calcular());actions.addView(calc,new LinearLayout.LayoutParams(0,dp(56),1));
        Button measure=new Button(this);measure.setText("MEDIR");measure.setTextSize(14);measure.setOnClickListener(v->measureCurrent(false));actions.addView(measure,new LinearLayout.LayoutParams(0,dp(56),1));
        Button register=new Button(this);register.setText("REGISTRO");register.setTextSize(14);register.setOnClickListener(v->measureCurrent(true));actions.addView(register,new LinearLayout.LayoutParams(0,dp(56),1));root.addView(actions);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selectedProgram=pos;cbMulti.setVisibility(pos==6||pos==7?View.VISIBLE:View.GONE);build(pos);}public void onNothingSelected(AdapterView<?> p){}});
        cbMulti.setOnCheckedChangeListener((v,c)->{if(selectedProgram==6||selectedProgram==7)build(selectedProgram);});setContentView(root);
    }
    private EditText edit(String hint,float weight){EditText e=new EditText(this);e.setInputType(NUM);e.setHint(hint);e.setSingleLine(true);if(weight>0)e.setLayoutParams(new LinearLayout.LayoutParams(0,-2,weight));return e;}
    private void build(int pos){cur=TopoMath.PROGS[pos];box.removeAllViews();fields.clear();kinds.clear();pointRows.clear();activePointRow=null;tvRes.setText("");multi=null;areaPlot=null;sectionPlot=null;sectionPlateName=null;areaClosed=false;
        if(pos==8){buildArea();styleProgramViews(programRoot);return;}
        if(pos==9){buildSectionForm();styleProgramViews(programRoot);return;}
        if(cbMulti.isChecked()&&(pos==6||pos==7)){TextView help=new TextView(this);help.setText("Una fila por PI: N;E;Z;Az entrada;Az salida;Radio"+(pos==7?";Ls":"")+". Separador ;");box.addView(help);multi=new EditText(this);multi.setHint("N;E;Z;Az entrada;Az salida;Radio"+(pos==7?";Ls":"")+"\n...");multi.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);multi.setMinLines(4);box.addView(multi);addLatestButton(true);styleProgramViews(programRoot);return;}
        for(String spec:cur.in){char t=spec.charAt(0);String label=spec.substring(2);if(t=='P'){
            TextView tv=new TextView(this);tv.setText(label);box.addView(tv);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            EditText n=edit("N",1),e=edit("E",1),z=edit("Z",1);row.addView(e);row.addView(n);row.addView(z);
            Button m=new Button(this);m.setText("Medir");m.setOnClickListener(v->measureInto(n,e,z));row.addView(m);
            Button pick=new Button(this);pick.setText("Leídos");pick.setOnClickListener(v->elegir(n,e,z));row.addView(pick);box.addView(row);
            EditText[] rowFields={n,e,z};pointRows.add(rowFields);for(EditText input:rowFields)input.setOnFocusChangeListener((view,focus)->{if(focus)activePointRow=rowFields;});fields.add(n);kinds.add('R');fields.add(e);kinds.add('R');fields.add(z);kinds.add('Z');
        }else{EditText e=edit(label+(t=='A'?" (°)":""),0);box.addView(e);fields.add(e);kinds.add(t=='A'?'A':'R');}}styleProgramViews(programRoot);
    }
    private void buildSectionForm(){
        addSectionPoint("Norte medido · Este medido · Altura medida",new String[]{"N medido","E medido","Z medida"});
        addSectionPoint("Punto eje · N · E · Z",new String[]{"N eje","E eje","Z eje"});
        addSectionPair("Azimut tangente","Kilometraje eje",'A','R');
        addSectionPair("Alzado de diseño (m)","Pendiente longitudinal (%)",'R','R');
        addSectionPair("Desplazamiento de diseño (m)","Espesor de capa (m)",'R','R');
        addSectionPair("Desplazamiento placa lateral (m)","Ángulo incluido (°)",'R','A');
        addSectionPair("Pendiente de placa (%)","Ensanchamiento de placa (m)",'R','R');
        buildSectionOptions();
        sectionPlot=new SectionView(this);box.addView(sectionPlot,new LinearLayout.LayoutParams(-1,dp(220)));
    }
    private void addSectionPoint(String title,String[] hints){TextView label=new TextView(this);label.setText(title);label.setTextSize(13);label.setPadding(0,8,0,2);box.addView(label);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);EditText[] coords=new EditText[3];
        for(int i=0;i<3;i++){coords[i]=edit(hints[i],1);row.addView(coords[i],new LinearLayout.LayoutParams(0,-2,1));}
        EditText[] point={coords[0],coords[1],coords[2]};pointRows.add(point);for(EditText input:point)input.setOnFocusChangeListener((view,focus)->{if(focus)activePointRow=point;});box.addView(row);
        for(int i=0;i<3;i++){fields.add(coords[i]);kinds.add(i==2?'Z':'R');}
    }
    private EditText sectionNumber(String label,char kind){EditText e=edit(kind=='A'?"0.0000°":"0.000",0);fields.add(e);kinds.add(kind);return e;}
    private void addSectionPair(String leftLabel,String rightLabel,char leftKind,char rightKind){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(0,2,0,2);LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
        TextView l=new TextView(this);l.setText(leftLabel);left.addView(l);left.addView(sectionNumber(leftLabel,leftKind));TextView r=new TextView(this);r.setText(rightLabel);right.addView(r);right.addView(sectionNumber(rightLabel,rightKind));row.addView(left,new LinearLayout.LayoutParams(0,-2,1));row.addView(right,new LinearLayout.LayoutParams(0,-2,1));box.addView(row);
    }
    private void buildSectionOptions(){TextView title=new TextView(this);title.setText("Datos de placa y alineamiento");title.setTextSize(15);title.setPadding(0,8,0,2);box.addView(title);
        sectionPlateName=edit("Nombre de placa perteneciente",0);box.addView(sectionPlateName);
        LinearLayout selectors=new LinearLayout(this);selectors.setOrientation(LinearLayout.HORIZONTAL);Spinner element=new Spinner(this);String[] elementNames={"Recta","Curva circular","Espiral"};element.setAdapter(darkAdapter(elementNames));element.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff303030));selectors.addView(element,new LinearLayout.LayoutParams(0,-2,1));element.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){TopoMath.sectionElement=elementNames[pos];}public void onNothingSelected(AdapterView<?> p){}});
        Spinner orientation=new Spinner(this);String[] orientations={"Gran kilometraje","Pequeño kilometraje"};orientation.setAdapter(darkAdapter(orientations));orientation.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff303030));selectors.addView(orientation,new LinearLayout.LayoutParams(0,-2,1));orientation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){TopoMath.sectionReverse=pos==1;if(sectionPlot!=null)sectionPlot.invalidate();}public void onNothingSelected(AdapterView<?> p){}});box.addView(selectors);
        TextView note=new TextView(this);note.setText("El gráfico muestra el eje, el punto medido y la posición objetivo. Introduce los datos del proyecto para calcular el replanteo.");note.setPadding(0,4,0,8);box.addView(note);
    }
    private ArrayAdapter<String> darkAdapter(String[] values){ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values){@Override public View getView(int position,View convert,android.view.ViewGroup parent){TextView t=(TextView)super.getView(position,convert,parent);t.setTextColor(0xffffffff);return t;}@Override public View getDropDownView(int position,View convert,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(position,convert,parent);t.setTextColor(0xffffffff);t.setBackgroundColor(0xff303030);t.setPadding(20,18,20,18);return t;}};a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return a;}
    private void styleProgramViews(View view){if(view instanceof CheckBox){((CheckBox)view).setTextColor(0xffffffff);}else if(view instanceof Button){Button b=(Button)view;b.setTextColor(0xffffffff);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff4caf50));}else if(view instanceof EditText){EditText e=(EditText)view;e.setTextColor(0xffffffff);e.setHintTextColor(0xffaaaaaa);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff9e9e9e));}else if(view instanceof TextView){((TextView)view).setTextColor(0xffffffff);}if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++)styleProgramViews(g.getChildAt(i));}}
    private void buildArea(){TextView help=new TextView(this);help.setText("Vértices E;N;Z. MEDIR agrega un vértice; REGISTRO lo mide y guarda. Repite el primero o pulsa Cerrar para calcular.");box.addView(help);multi=new EditText(this);multi.setHint("E;N;Z\nE;N;Z\nE;N;Z");multi.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);multi.setMinLines(3);box.addView(multi);multi.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){areaClosed=false;refreshAreaPreview();}public void afterTextChanged(Editable e){}});Button close=new Button(this);close.setText("Cerrar polígono · Calcular área 2D/3D");close.setOnClickListener(v->closeArea());box.addView(close);addLatestButton(false);areaPlot=new PolygonView(this);box.addView(areaPlot,new LinearLayout.LayoutParams(-1,dp(200)));}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private void addLatestButton(boolean pi){Button b=new Button(this);b.setText("Añadir último punto medido");b.setOnClickListener(v->{if(Double.isNaN(Store.lastN)){Toast.makeText(this,"Primero mide un punto desde la app",Toast.LENGTH_SHORT).show();return;}String line=selectedProgram==8?String.format(Locale.US,"%.3f;%.3f;%.3f",Store.lastE,Store.lastN,Store.lastZ):String.format(Locale.US,"%.3f;%.3f;%.3f",Store.lastN,Store.lastE,Store.lastZ);if(pi)line+=selectedProgram==7?";;;;":";;;";multi.append((multi.length()==0?"":"\n")+line);if(selectedProgram==8)checkAreaClosed();});box.addView(b);}
    private void measureInto(EditText n,EditText e,EditText z){if(Store.measureAction==null){Toast.makeText(this,"Conecta la estación desde la pantalla principal",Toast.LENGTH_SHORT).show();return;}Store.pointListener=p->{n.setText(String.format(Locale.US,"%.3f",p[0]));e.setText(String.format(Locale.US,"%.3f",p[1]));z.setText(String.format(Locale.US,"%.3f",p[2]));};Store.measureAction.run();}
    private void measureCurrent(boolean save){Runnable action=save?Store.saveMeasureAction:Store.measureAction;if(action==null){Toast.makeText(this,"Vuelve a la pantalla principal y conecta la estación",Toast.LENGTH_SHORT).show();return;}
        if(selectedProgram==8){if(areaClosed){Toast.makeText(this,"El polígono ya está cerrado",Toast.LENGTH_SHORT).show();return;}Store.pointListener=p->appendAreaPoint(p);action.run();return;}
        if(multi!=null&&(selectedProgram==6||selectedProgram==7)){Store.pointListener=p->{String line=String.format(Locale.US,"%.3f;%.3f;%.3f",p[0],p[1],p[2])+(selectedProgram==7?";;;;":";;;");multi.append((multi.length()==0?"":"\n")+line);};action.run();return;}
        if(pointRows.isEmpty()){action.run();return;}EditText[] target=activePointRow!=null?activePointRow:pointRows.get(0);final EditText[] dst=target;
        Store.pointListener=p->{dst[0].setText(String.format(Locale.US,"%.3f",p[0]));dst[1].setText(String.format(Locale.US,"%.3f",p[1]));dst[2].setText(String.format(Locale.US,"%.3f",p[2]));};action.run();
    }
    private void appendAreaPoint(double[] p){if(areaClosed||multi==null)return;String line=String.format(Locale.US,"%.3f;%.3f;%.3f",p[1],p[0],p[2]);multi.append((multi.length()==0?"":"\n")+line);if(!checkAreaClosed())tvRes.setText("Vértice agregado: "+areaLineCount()+". Repite el primer vértice o cierra el polígono para ver el área.");}
    private int areaLineCount(){int n=0;for(String line:multi.getText().toString().split("\\r?\\n"))if(!line.trim().isEmpty())n++;return n;}
    private double[][] readAreaPoints(){List<double[]> points=new ArrayList<>();String[] lines=multi.getText().toString().split("\\r?\\n");for(String line:lines){line=line.trim();if(line.isEmpty())continue;String[] a=line.split("[;\\t]",-1);if(a.length<3)throw new IllegalArgumentException("Cada vértice requiere E;N;Z: "+line);try{double e=Double.parseDouble(a[0].trim().replace(',','.')),n=Double.parseDouble(a[1].trim().replace(',','.')),z=Double.parseDouble(a[2].trim().replace(',','.'));points.add(new double[]{n,e,z});}catch(Exception ex){throw new IllegalArgumentException("Dato inválido en vértice: "+line);}}return points.toArray(new double[0][]);}
    private void refreshAreaPreview(){if(areaPlot==null)return;try{areaPlot.setPoints(readAreaPoints(),false);checkAreaClosed();}catch(Exception ignored){areaPlot.setPoints(null,false);}}
    private boolean checkAreaClosed(){try{double[][] p=readAreaPoints();if(p.length>=4&&TopoMath.samePoint(p[0],p[p.length-1])){areaClosed=true;if(areaPlot!=null)areaPlot.setPoints(p,true);tvRes.setText(TopoMath.area(p));return true;}}catch(Exception ignored){}return false;}
    private void closeArea(){try{double[][] p=readAreaPoints();if(p.length<3){tvRes.setText("Ingresa al menos 3 vértices antes de cerrar el polígono.");return;}areaClosed=true;if(areaPlot!=null)areaPlot.setPoints(p,true);tvRes.setText(TopoMath.area(p));}catch(Exception ex){tvRes.setText(ex.getMessage());}}
    private void elegir(final EditText n,final EditText e,final EditText z){if(Store.points.isEmpty()){Toast.makeText(this,"Aún no hay puntos leídos",Toast.LENGTH_SHORT).show();return;}String[] items=new String[Store.points.size()];for(int i=0;i<items.length;i++){double[] q=Store.points.get(i);items[i]=String.format(Locale.US,"%s E=%.3f N=%.3f Z=%.3f",Store.names.get(i),q[1],q[0],q[2]);}
        new AlertDialog.Builder(this).setTitle("Puntos leídos").setItems(items,(d,i)->{double[] q=Store.points.get(i);n.setText(String.format(Locale.US,"%.3f",q[0]));e.setText(String.format(Locale.US,"%.3f",q[1]));z.setText(String.format(Locale.US,"%.3f",q[2]));}).show();}
    private void calcular(){if(selectedProgram==8){closeArea();return;}if(multi!=null){calcularMultiple();return;}double[] v=new double[fields.size()];for(int i=0;i<v.length;i++){String s=fields.get(i).getText().toString().trim().replace(',','.');char k=kinds.get(i);if(s.isEmpty()||s.equals("-")||s.equals(".")){if(k=='Z'&&selectedProgram!=9){v[i]=Double.NaN;continue;}tvRes.setText("Faltan datos por llenar.");return;}try{v[i]=Double.parseDouble(s);}catch(Exception ex){tvRes.setText("Número no válido: "+s);return;}if(k=='A')v[i]=TopoMath.ang(v[i]);}
        try{if(selectedProgram==9){TopoMath.sectionPlateName=sectionPlateName.getText().toString().trim().isEmpty()?"Placa objetivo":sectionPlateName.getText().toString().trim();TopoMath.SectionResult r=TopoMath.sectionResult(v);sectionPlot.setResult(r);}tvRes.setText(cur.run.run(v));}catch(Exception ex){tvRes.setText("Error de cálculo: "+ex.getMessage());}}
    private void calcularMultiple(){String[] lines=multi.getText().toString().split("\\r?\\n");List<double[]> pts=new ArrayList<>();StringBuilder result=new StringBuilder();boolean area=selectedProgram==8;int required=area?2:(selectedProgram==7?7:6);
        for(String line:lines){line=line.trim();if(line.isEmpty())continue;String[] parts=line.split("[;\\t]",-1);if(parts.length<required){tvRes.setText("Fila incompleta: "+line);return;}double[] a=new double[parts.length];try{int lim=area?Math.min(2,parts.length):parts.length;for(int i=0;i<lim;i++)a[i]=Double.parseDouble(parts[i].trim().replace(',','.'));if(!area)for(int i=0;i<a.length;i++)if(parts[i].trim().isEmpty())throw new NumberFormatException();}catch(Exception ex){tvRes.setText("Dato no numérico: "+line);return;}
            if(area){pts.add(new double[]{a[1],a[0],Double.NaN});continue;}
            double az1=TopoMath.ang(a[3]),az2=TopoMath.ang(a[4]);double[] v=selectedProgram==7?new double[]{a[0],a[1],a[2],az1,az2,a[5],a[6]}:new double[]{a[0],a[1],a[2],az1,az2,a[5]};
            result.append("PI ").append(pts.size()+1).append("\n").append(cur.run.run(v)).append("\n");pts.add(new double[]{a[0],a[1],a[2]});
        }
        if(area){double[][] a=pts.toArray(new double[0][]);tvRes.setText(TopoMath.area(a));}else if(pts.isEmpty())tvRes.setText("Ingrese al menos un PI.");else tvRes.setText(result.toString());
    }
    @Override protected void onDestroy(){super.onDestroy();Store.pointListener=null;}
}
