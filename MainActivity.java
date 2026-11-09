package com.example.topobt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int EXPORT_TXT=71;
    private BluetoothAdapter adapter; private BluetoothSocket socket; private OutputStream out; private BluetoothDevice activeDevice;
    private volatile boolean connected=false, saveNext=false, measurementPending=false;
    private long measurementSentAt=0;
    private final Handler uiHandler=new Handler(Looper.getMainLooper());
    private final Runnable measurementTimeout=()->{if(measurementPending&&System.currentTimeMillis()-measurementSentAt>=7000){measurementPending=false;saveNext=false;logCommunication("TIMEOUT","No llegó respuesta a la solicitud de medición.");tvStatus.setText("Sin respuesta de medición. Revisa el perfil y el modo de comunicación de la estación; mira el registro TX/RX.");}};
    private final List<BluetoothDevice> devices=new ArrayList<>();
    private final StringBuilder csv=new StringBuilder("hora,dato,punto,E,N,Z\n");
    private final SimpleDateFormat hora=new SimpleDateFormat("HH:mm:ss",Locale.US);
    private Spinner spDevices,spProfile; private TextView tvLog,tvStatus,tvLive; private ScrollView scroll;
    private Button btnConnect,btnTarget; private StationView stationView;
    private File rootDir, activeFile;
    private final StringBuilder gsiBuffer=new StringBuilder();
    private static final String[] PROFILES={"Automático (por estación)","Sokkia CX (SDR)","Topcon GM (SDR)","Topcon ES-105 (SDR)","Leica TS / TCR / TCRM (GSI)","GeoMax (GSI)","Topcon GTS-323 (GTS)"};
    private volatile int activeProfile=0;

    private String now(){synchronized(hora){return hora.format(new Date());}}
    private void logCommunication(String direction,String message){String entry=now()+"  "+direction+"  "+message+"\n";runOnUiThread(()->{tvLog.append(entry);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});}

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);
        spDevices=findViewById(R.id.spDevices);spProfile=findViewById(R.id.spProfile);tvLog=findViewById(R.id.tvLog);
        tvStatus=findViewById(R.id.tvStatus);tvLive=findViewById(R.id.tvLive);scroll=findViewById(R.id.scroll);
        btnConnect=findViewById(R.id.btnConnect);btnTarget=findViewById(R.id.btnTarget);stationView=findViewById(R.id.stationView);
        spProfile.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PROFILES));
        spDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position>=0&&position<devices.size()){int remembered=getPreferences(MODE_PRIVATE).getInt(profileKey(devices.get(position)),0);spProfile.setSelection(remembered>0&&remembered<PROFILES.length?remembered:0);}}public void onNothingSelected(AdapterView<?> parent){}});
        spProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(connected&&position>0){activeProfile=position;if(activeDevice!=null)getPreferences(MODE_PRIVATE).edit().putInt(profileKey(activeDevice),position).apply();tvStatus.setText("Conectado · "+(activeDevice==null?"estación":activeDevice.getName())+" · "+PROFILES[position]);}}public void onNothingSelected(AdapterView<?> parent){}});
        adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null){tvStatus.setText("Este equipo no tiene Bluetooth");return;}
        rootDir=new File(getExternalFilesDir(null),"TopoBT");rootDir.mkdirs();String savedFolder=getPreferences(MODE_PRIVATE).getString("folder",Store.folder);setFolder(savedFolder);
        btnConnect.setOnClickListener(v->{if(connected)disconnect();else connect();});
        findViewById(R.id.btnStation).setOnClickListener(v->stationDialog());
        findViewById(R.id.btnTools).setOnClickListener(v->startActivity(new Intent(this,ToolsActivity.class)));
        findViewById(R.id.btnMeasure).setOnClickListener(v->requestMeasurement(false));
        findViewById(R.id.btnMeasureSave).setOnClickListener(v->requestMeasurement(true));
        btnTarget.setOnClickListener(v->targetDialog());updateTargetButton();
        findViewById(R.id.btnFolder).setOnClickListener(v->folderDialog());
        findViewById(R.id.btnClear).setOnClickListener(v->tvLog.setText(""));
        Store.measureAction=()->requestMeasurement(false);
        Store.saveMeasureAction=()->requestMeasurement(true);
        if(hasPermission())loadDevices();else requestPerm();
    }

    private boolean hasPermission(){return Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void requestPerm(){if(Build.VERSION.SDK_INT>=31)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},1);}
    @Override public void onRequestPermissionsResult(int c,String[] p,int[] r){super.onRequestPermissionsResult(c,p,r);if(r.length>0&&r[0]==PackageManager.PERMISSION_GRANTED)loadDevices();else tvStatus.setText("Falta el permiso de Bluetooth");}
    private void loadDevices(){if(!adapter.isEnabled()){tvStatus.setText("Activa Bluetooth y vuelve a abrir la app");return;}devices.clear();List<String> names=new ArrayList<>();
        for(BluetoothDevice d:adapter.getBondedDevices()){devices.add(d);names.add(d.getName()+"\n"+d.getAddress());}
        if(names.isEmpty())tvStatus.setText("Empareja primero la estación en Ajustes > Bluetooth");
        spDevices.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));}
    private int resolveProfile(String deviceName){String n=deviceName==null?"":deviceName.toUpperCase(Locale.ROOT);
        if(n.contains("LEICA")||n.contains("TCRM")||n.startsWith("TS")||n.startsWith("TPS")||n.startsWith("TCR")||n.startsWith("TC"))return 4;
        if(n.contains("GEOMAX")||n.contains("ZOOM"))return 5;
        if(n.contains("SOKKIA")||n.contains("CX"))return 1;
        if(n.contains("ES-105")||n.contains("ES105")||n.contains("TOPCON ES"))return 3;
        if(n.contains("GM-")||n.contains("GM_")||n.contains("TOPCON GM"))return 2;
        if(n.contains("GTS-323")||n.contains("GTS323")||n.contains("323"))return 6;
        return 0;
    }
    private String profileKey(BluetoothDevice device){return "profile_"+device.getAddress().replace(":","");}
    private void connect(){int pos=spDevices.getSelectedItemPosition();if(pos<0||pos>=devices.size())return;BluetoothDevice dev=devices.get(pos);activeDevice=dev;final int selectedProfile=spProfile.getSelectedItemPosition();tvStatus.setText("Conectando...");btnConnect.setEnabled(false);
        new Thread(()->{try{socket=dev.createRfcommSocketToServiceRecord(SPP);socket.connect();out=socket.getOutputStream();connected=true;
            int remembered=getPreferences(MODE_PRIVATE).getInt(profileKey(dev),0);
            activeProfile=selectedProfile>0?selectedProfile:(remembered>0?remembered:resolveProfile(dev.getName()));
            if(activeProfile>0)getPreferences(MODE_PRIVATE).edit().putInt(profileKey(dev),activeProfile).apply();
            final int profile=activeProfile;
            runOnUiThread(()->{spDevices.setEnabled(false);if(profile>0)spProfile.setSelection(profile);String label=profile==0?"Equipo sin perfil · selecciona marca/modelo":profile==6?"Topcon GTS-323 · perfil GTS sin comando remoto documentado":PROFILES[profile];tvStatus.setText("Conectado · "+dev.getName()+" · "+label);btnConnect.setText("Desconectar");btnConnect.setEnabled(true);});readLoop(socket.getInputStream());
        }catch(Exception e){runOnUiThread(()->{tvStatus.setText("Error de conexión: "+e.getMessage());btnConnect.setEnabled(true);});}finally{closeQuietly();}}).start();}
    private void readLoop(InputStream in){StringBuilder line=new StringBuilder();byte[] b=new byte[256];try{int n;while(connected&&(n=in.read(b))>0)for(int i=0;i<n;i++){int x=b[i]&255;char c=(char)x;
            // Algunos equipos terminan la respuesta con CR solamente; otros usan CR/LF.
            // Procesar ambos evita dejar la medición retenida esperando un LF inexistente.
            if(c=='\r'||c=='\n'||x==3){String s=line.toString().trim();line.setLength(0);if(!s.isEmpty())onLine(s);}else if(x>=32&&x!=127)line.append(c);
        }}catch(IOException ignored){}}

    private void requestMeasurement(boolean save){if(!connected||out==null){Toast.makeText(this,"Conecta primero la estación",Toast.LENGTH_SHORT).show();return;}
        if(measurementPending&&System.currentTimeMillis()-measurementSentAt<8000){if(save)saveNext=true;tvStatus.setText("Esperando respuesta de la medición anterior…");return;}
        int profile=activeProfile;
        if(profile==0){Toast.makeText(this,"Selecciona el modelo de esta estación antes de medir",Toast.LENGTH_LONG).show();tvStatus.setText("Selecciona el perfil de estación y vuelve a pulsar Medir");return;}
        if(profile==6){Toast.makeText(this,"El Topcon GTS-323 necesita su manual de interfaz GTS para iniciar la medición desde la app",Toast.LENGTH_LONG).show();tvStatus.setText("GTS-323 conectado; falta confirmar el comando remoto de su interfaz GTS");return;}
        boolean gsi=profile==4||profile==5;
        if(gsi)gsiBuffer.setLength(0);
        saveNext=save;measurementPending=true;measurementSentAt=System.currentTimeMillis();uiHandler.removeCallbacks(measurementTimeout);uiHandler.postDelayed(measurementTimeout,7000);tvStatus.setText(save?"Midiendo; se guardará al recibir respuesta…":"Solicitando una lectura…");
        final String requestLabel=gsi?"GET/M/WI21/WI22/WI31 <CR/LF>":"0x11 (distancia y ángulos) <CR/LF>";
        final byte[] command=gsi?"GET/M/WI21/WI22/WI31\r\n".getBytes(StandardCharsets.US_ASCII):new byte[]{0x11,'\r','\n'};
        logCommunication("TX",requestLabel);
        new Thread(()->{try{out.write(command);out.flush();runOnUiThread(()->{if(measurementPending)tvStatus.setText("Solicitud enviada; esperando respuesta de la estación…");});}catch(IOException e){measurementPending=false;saveNext=false;uiHandler.removeCallbacks(measurementTimeout);logCommunication("TX-ERROR",String.valueOf(e.getMessage()));runOnUiThread(()->tvStatus.setText("Error al enviar la medición: "+e.getMessage()));}}).start();
    }
    private void onLine(String s){String t=now();
        int profile=activeProfile;
        double sd=Double.NaN,vz=Double.NaN,hz=Double.NaN;
        if(profile>=1&&profile<=3){double[] q=parseSokkia(s);if(q!=null){sd=q[0];vz=q[1];hz=q[2];}}
        else if(profile==4||profile==5){if(gsiBuffer.length()>0)gsiBuffer.append(' ');gsiBuffer.append(s);double[] q=parseGsi(gsiBuffer.toString());if(q==null){if(gsiBuffer.length()>512)gsiBuffer.setLength(0);final String raw=t+"  RX  "+s+"\n";runOnUiThread(()->tvLog.append(raw));return;}s=gsiBuffer.toString().trim();gsiBuffer.setLength(0);hz=q[0];vz=q[1];sd=q[2];}
        else if(profile==6){final String raw=t+"  RX  "+s+"\n";runOnUiThread(()->tvLog.append(raw));return;}
        String name="",ce="",cn="",cz="",extra="";
        if(!Double.isNaN(sd)&&!Double.isNaN(vz)&&!Double.isNaN(hz)&&sd>0){measurementPending=false;uiHandler.removeCallbacks(measurementTimeout);
            Store.lastSd=sd;Store.lastHz=hz;Store.lastVz=vz;
            boolean stationReady=finite(Store.n0)&&finite(Store.e0)&&finite(Store.z0)&&finite(Store.az0);
            if(stationReady){
                double az=Math.toRadians(Store.az0+hz),hd=sd*Math.sin(Math.toRadians(vz)),dz=sd*Math.cos(Math.toRadians(vz))+Store.hi-Store.ht;
                double n=Store.n0+hd*Math.cos(az),e=Store.e0+hd*Math.sin(az),z=Store.z0+dz;
                Store.lastN=n;Store.lastE=e;Store.lastZ=z;name=Store.add(n,e,z);Store.lastPointName=name;ce=fmt(e);cn=fmt(n);cz=fmt(z);
                if(Store.pointListener!=null){Store.PointListener listener=Store.pointListener;Store.pointListener=null;double[] point={n,e,z};runOnUiThread(()->listener.onPoint(point));}
                extra="\n   → "+name+"  E="+ce+"  N="+cn+"  Z="+cz;
                if(saveNext){persistPoint(t,s,name,e,n,z);saveNext=false;}
                final String live=String.format(Locale.US,"D: %.3f m   |   Azimut Hz: %.4f°   |   V: %.4f°\nE: %.3f   |   N: %.3f   |   Z: %.3f",sd,hz,vz,e,n,z);
                runOnUiThread(()->{tvLive.setText(live);stationView.invalidate();tvStatus.setText("Medición recibida");});
            }else{
                Store.lastN=Store.lastE=Store.lastZ=Double.NaN;
                Store.pointListener=null;
                if(saveNext){persistRawObservation(t,s);saveNext=false;runOnUiThread(()->Toast.makeText(this,"Lectura guardada. Configura la estación cuando necesites E/N/Z.",Toast.LENGTH_LONG).show());}
                final String live=String.format(Locale.US,"D: %.3f m   |   Azimut Hz: %.4f°   |   V: %.4f°\nE: —   |   N: —   |   Z: —",sd,hz,vz);
                runOnUiThread(()->{tvLive.setText(live);stationView.invalidate();tvStatus.setText("Lectura recibida; falta configurar Estación");});
            }
        }else{measurementPending=false;uiHandler.removeCallbacks(measurementTimeout);runOnUiThread(()->tvStatus.setText("Llegaron datos, pero el formato no se reconoció; revisa la línea RX del registro."));}
        csv.append(t).append(",\"").append(s.replace("\"","\"\"")).append("\",").append(name).append(",").append(ce).append(",").append(cn).append(",").append(cz).append("\n");
        final String outLine=t+"  RX  "+s+extra+"\n";runOnUiThread(()->{tvLog.append(outLine);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});
    }
    private double[] parseGsi(String s){double hz=Double.NaN,vz=Double.NaN,sd=Double.NaN;
        Matcher m=Pattern.compile("(21|22|31)([.\\w]{0,4})([+-])(\\d{1,16})").matcher(s);
        while(m.find()){int wi=Integer.parseInt(m.group(1));double val;try{val=Double.parseDouble(m.group(4));}catch(Exception ex){continue;}if("-".equals(m.group(3)))val=-val;
            String info=m.group(2);int unit=info.isEmpty()?-1:Character.digit(info.charAt(info.length()-1),10);
            if(wi==21)hz=gsiAngle(val,unit);else if(wi==22)vz=gsiAngle(val,unit);else if(wi==31)sd=gsiDistance(val,unit);
        }
        return Double.isNaN(hz)||Double.isNaN(vz)||Double.isNaN(sd)?null:new double[]{hz,vz,sd};}
    private double[] parseSokkia(String s){String raw=s.trim();
        // Algunos equipos entregan el registro SET/Sokkia separado por comas:
        // Ea, estado, altura prisma, ppm, distancia, cenital, Hz, checksum.
        if(raw.regionMatches(true,0,"Ea",0,2)&&raw.contains(",")){try{String[] a=raw.split(",",-1);if(a.length>=7){double d=Double.parseDouble(a[4].trim().replace(',','.'));double v=angleField(a[5]),h=angleField(a[6]);if(d>0)return new double[]{d,v,h};}}catch(Exception ignored){}}
        String[] f=raw.replace('_',' ').trim().split("\\s+");
        // En algunos firmwares los tres campos fijos de siete caracteres llegan pegados.
        if(f.length<3){String compact=raw.replaceAll("[^0-9+\\-]","");if(compact.length()<21)return null;f=new String[]{compact.substring(0,7),compact.substring(7,14),compact.substring(14,21)};}
        try{
        double d=fixedNumber(f[0],true),v=fixedNumber(f[1],false),h=fixedNumber(f[2],false);
        if(d<=0)return null;return new double[]{d,v,h};
    }catch(Exception e){return null;}}
    private double angleField(String token){String t=token.trim();return t.contains(".")?dmsToDegrees(t):fixedNumber(t,false);}
    private double fixedNumber(String token,boolean distance){String t=token.replaceAll("[^0-9.+-]","");boolean neg=t.startsWith("-");String digits=t.replaceAll("[^0-9]","");
        if(t.contains(".")){double x=Double.parseDouble(t);return distance?x:(x<0?-dmsToDegrees(Double.toString(Math.abs(x))):dmsToDegrees(Double.toString(Math.abs(x))));}
        if(distance){double x=Double.parseDouble(t);return x/1000.0;}
        if(digits.length()<6)return Double.parseDouble(t);
        int n=digits.length(),deg=Integer.parseInt(digits.substring(0,n-4)),min=Integer.parseInt(digits.substring(n-4,n-2));
        double sec=Double.parseDouble(digits.substring(n-2));double x=deg+min/60.0+sec/3600.0;return neg?-x:x;
    }
    private double dmsToDegrees(String x){double a=Double.parseDouble(x);int d=(int)a;double tail=(a-d)*100;int m=(int)tail;double sec=(tail-m)*100;return d+m/60.0+sec/3600.0;}
    private double gsiAngle(double raw,int unit){double a=Math.abs(raw),deg;
        if(unit==2)deg=a/100000.0*0.9; // 400 gon -> 360 degrees
        else if(unit==3)deg=a/100000.0; // decimal degrees
        else if(unit==4){long v=Math.round(a);int d=(int)(v/100000);long tail=v-(long)d*100000;int min=(int)(tail/1000);double sec=(tail-min*1000)/10.0;deg=d+min/60.0+sec/3600.0;}
        else if(unit==5)deg=a/10000.0*360.0/6400.0; // mils
        else deg=a/100000.0;
        return raw<0?-deg:deg;}
    private double gsiDistance(double raw,int unit){double a=Math.abs(raw),metres;
        if(unit==6)metres=a/10000.0;else if(unit==8)metres=a/100000.0;else if(unit==1)metres=a/1000.0*0.3048;else if(unit==7)metres=a/10000.0*0.3048;else metres=a/1000.0;
        return raw<0?-metres:metres;}
    private String fmt(double v){return String.format(Locale.US,"%.3f",v);}
    private boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}

    private void stationDialog(){ScrollView sv=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(32,16,32,0);sv.addView(l);
        String[] labels={"Este de la estación","Norte de la estación","Cota de la estación","Azimut de Hz=0","Altura del instrumento","Altura del prisma"};double[] vals={Store.e0,Store.n0,Store.z0,Store.az0,Store.hi,Store.ht};EditText[] ed=new EditText[6];
        for(int i=0;i<6;i++){ed[i]=new EditText(this);ed[i].setHint(labels[i]);ed[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);ed[i].setText(Double.isNaN(vals[i])?"":String.valueOf(vals[i]));l.addView(ed[i]);}
        CheckBox cb=new CheckBox(this);cb.setText("Ángulos en GG.MMSS");cb.setChecked(TopoMath.gms);l.addView(cb);
        new AlertDialog.Builder(this).setTitle("Datos de estación (se calculan con la lectura; completar estación y orientación)").setView(sv).setPositiveButton("Guardar",(d,w)->{try{
            Store.e0=val(ed[0]);Store.n0=val(ed[1]);Store.z0=val(ed[2]);Store.az0=TopoMath.ang(val(ed[3]));Store.hi=val(ed[4]);Store.ht=Store.reflectorless?0:val(ed[5]);TopoMath.gms=cb.isChecked();updateTargetButton();
        }catch(Exception ex){Toast.makeText(this,"Revisa los datos numéricos",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Cancelar",null).show();}
    private void targetDialog(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(32,16,32,0);
        RadioGroup rg=new RadioGroup(this);RadioButton prism=new RadioButton(this),noPrism=new RadioButton(this);prism.setText("Prisma");noPrism.setText("Sin prisma");rg.addView(prism);rg.addView(noPrism);l.addView(rg);
        EditText height=new EditText(this);height.setHint("Altura del prisma (m)");height.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);height.setText(Store.reflectorless||!finite(Store.ht)?"0.000":fmt(Store.ht));l.addView(height);
        prism.setChecked(!Store.reflectorless);noPrism.setChecked(Store.reflectorless);height.setEnabled(!Store.reflectorless);
        rg.setOnCheckedChangeListener((g,id)->height.setEnabled(id==prism.getId()));
        TextView note=new TextView(this);note.setText(activeProfile==4?"Para Leica se enviará el cambio de modo EDM al equipo. Verifica el modo que queda activo en la estación.":"La altura se usa en los cálculos. Este perfil no tiene aquí un comando remoto de prisma confirmado para la estación.");l.addView(note);
        new AlertDialog.Builder(this).setTitle("Objetivo y altura").setView(l).setPositiveButton("Aplicar",(d,w)->{try{Store.reflectorless=noPrism.isChecked();Store.ht=Store.reflectorless?0:Double.parseDouble(height.getText().toString().trim().replace(',','.'));updateTargetButton();applyTargetToInstrument();}catch(Exception ex){Toast.makeText(this,"Ingresa una altura válida",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Cancelar",null).show();}
    private void updateTargetButton(){if(btnTarget!=null)btnTarget.setText(Store.reflectorless?"Sin prisma":"Prisma · H="+fmt(Store.ht)+" m");}
    private double val(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
    private void applyTargetToInstrument(){if(!connected||out==null){tvStatus.setText("Objetivo guardado en la app; conecta la estación para enviar el modo EDM");return;}
        if(activeProfile!=4){tvStatus.setText("Altura aplicada en la app; cambia el modo de objetivo en la estación");return;}
        final String mode=Store.reflectorless?"SET/161/7":"SET/161/0";
        final String aim=Store.reflectorless?"SET/130/5":"SET/130/1";
        new Thread(()->{try{out.write((aim+"\r\n"+mode+"\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();runOnUiThread(()->tvStatus.setText("Cambio de objetivo enviado a Leica; verifica el modo EDM en la estación"));}
            catch(IOException e){runOnUiThread(()->tvStatus.setText("No se pudo enviar el modo de objetivo al Leica"));}}).start();}
    private void disconnect(){connected=false;measurementPending=false;saveNext=false;uiHandler.removeCallbacks(measurementTimeout);closeQuietly();}
    private void closeQuietly(){connected=false;measurementPending=false;uiHandler.removeCallbacks(measurementTimeout);try{if(socket!=null)socket.close();}catch(IOException ignored){}runOnUiThread(()->{spDevices.setEnabled(true);activeDevice=null;btnConnect.setText("Conectar");btnConnect.setEnabled(true);if(!tvStatus.getText().toString().startsWith("Error"))tvStatus.setText("Desconectado");});}

    private void setFolder(String name){if(name==null||name.trim().isEmpty())name="Levantamiento";Store.folder=name.replaceAll("[^A-Za-z0-9 _-]","_");getPreferences(MODE_PRIVATE).edit().putString("folder",Store.folder).apply();File d=new File(rootDir,Store.folder);d.mkdirs();activeFile=new File(d,"puntos.txt");loadFolderPoints();}
    private void loadFolderPoints(){Store.points.clear();Store.names.clear();if(!activeFile.exists())return;try(BufferedReader r=new BufferedReader(new FileReader(activeFile))){String line;boolean first=true;while((line=r.readLine())!=null){if(first){first=false;continue;}String[] a=line.split(";",-1);if(a.length<5)continue;try{double e=Double.parseDouble(a[2]),n=Double.parseDouble(a[3]),z=Double.parseDouble(a[4]);Store.names.add(a[1]);Store.points.add(new double[]{n,e,z});}catch(Exception ignored){}}}catch(IOException ignored){}}
    private void folderDialog(){File[] dirs=rootDir.listFiles(File::isDirectory);List<String> names=new ArrayList<>();if(dirs!=null)for(File f:dirs)names.add(f.getName());int newFolderIndex=names.size();names.add("+ Crear carpeta nueva");int exportIndex=names.size();names.add("Exportar TXT de la carpeta actual");String[] a=names.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Archivos · carpeta actual: "+Store.folder).setItems(a,(d,i)->{if(i==newFolderIndex)newFolder();else if(i==exportIndex)exportTxt();else{setFolder(a[i]);Toast.makeText(this,"Carpeta: "+Store.folder,Toast.LENGTH_SHORT).show();}}).show();}
    private void newFolder(){EditText e=new EditText(this);e.setHint("Nombre de carpeta");new AlertDialog.Builder(this).setTitle("Nueva carpeta").setView(e).setPositiveButton("Crear",(d,w)->{setFolder(e.getText().toString());Toast.makeText(this,"Carpeta creada: "+Store.folder,Toast.LENGTH_SHORT).show();}).setNegativeButton("Cancelar",null).show();}
    private void persistPoint(String time,String raw,String name,double east,double north,double z){try{if(!activeFile.exists())try(FileWriter fw=new FileWriter(activeFile,true)){fw.write("hora;punto;E;N;Z;lectura\n");}
        try(FileWriter fw=new FileWriter(activeFile,true)){fw.write(time+";"+name+";"+fmt(east)+";"+fmt(north)+";"+fmt(z)+";"+raw.replace(';',',')+"\n");}
        runOnUiThread(()->Toast.makeText(this,"Guardado en "+Store.folder+"/puntos.txt",Toast.LENGTH_SHORT).show());
    }catch(IOException e){runOnUiThread(()->Toast.makeText(this,"No se pudo guardar el punto",Toast.LENGTH_SHORT).show());}}
    private void persistRawObservation(String time,String raw){try{if(!activeFile.exists())try(FileWriter fw=new FileWriter(activeFile,true)){fw.write("hora;punto;E;N;Z;lectura\n");}try(FileWriter fw=new FileWriter(activeFile,true)){fw.write(time+";;;;;"+raw.replace(';',',')+"\n");}}catch(IOException e){runOnUiThread(()->Toast.makeText(this,"No se pudo guardar la lectura",Toast.LENGTH_SHORT).show());}}
    private void exportTxt(){if(!activeFile.exists()||activeFile.length()==0){Toast.makeText(this,"La carpeta no tiene puntos guardados todavía",Toast.LENGTH_SHORT).show();return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,Store.folder+"_puntos.txt");startActivityForResult(i,EXPORT_TXT);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==EXPORT_TXT&&res==RESULT_OK&&data!=null){try(InputStream in=new FileInputStream(activeFile);OutputStream o=getContentResolver().openOutputStream(data.getData())){if(o==null)throw new IOException("No se pudo crear archivo");byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);Toast.makeText(this,"TXT exportado",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"No se pudo exportar: "+e.getMessage(),Toast.LENGTH_LONG).show();}}}
    @Override protected void onDestroy(){super.onDestroy();disconnect();}
}
