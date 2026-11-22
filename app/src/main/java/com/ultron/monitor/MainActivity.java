package com.ultron.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status, cpu, gpu, ram, disk, temps, net, updated;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() { public void run() { readStats(); handler.postDelayed(this, 1000); } };
    private int red = Color.rgb(255, 35, 48), white = Color.WHITE, muted = Color.rgb(170,170,170);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(4 | 1024 | 4096);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,12,18,12); root.setBackgroundColor(Color.rgb(8,8,10));
        TextView title = text("ULTRON  //  PC MONITOR", 25, red, true); root.addView(title);
        status = text("● AGUARDANDO CONEXÃO USB / ADB", 14, Color.YELLOW, true); root.addView(status);
        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        cpu = card("CPU"); gpu = card("GPU"); ram = card("RAM"); disk = card("DISCO");
        row1.addView(cpu, weight()); row1.addView(gpu, weight()); row1.addView(ram, weight()); row1.addView(disk, weight()); root.addView(row1, new LinearLayout.LayoutParams(-1,0,3));
        temps = panel("TEMPERATURAS"); root.addView(temps, new LinearLayout.LayoutParams(-1,0,2));
        net = panel("REDE / INTERNET"); root.addView(net, new LinearLayout.LayoutParams(-1,0,2));
        updated = text("Conecte o tablet ao PC com depuração USB ativada.", 12, muted, false); root.addView(updated);
        setContentView(root); handler.post(refresh);
    }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -1, 1); }
    private TextView text(String s,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(10,8,10,8); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private TextView card(String name) { TextView t=text(name+"\n--",20,white,true); t.setBackgroundColor(Color.rgb(25,18,20)); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private TextView panel(String name) { TextView t=text(name+"\nAguardando dados...",17,white,true); t.setBackgroundColor(Color.rgb(19,17,19)); return t; }
    private String val(JSONObject j,String key,String suffix) { String v=j.optString(key, "N/D"); return v.equals("N/D")?v:v+suffix; }
    private void readStats() {
        SharedPreferences p=getSharedPreferences("stats",MODE_PRIVATE); String raw=p.getString("json",null); long received=p.getLong("received",0);
        if(raw==null || received==0) { status.setText("● SEM DADOS — INICIE O ULTRON BRIDGE NO WINDOWS"); return; }
        long age=System.currentTimeMillis()-received;
        if(age>6000) status.setText("● CONEXÃO PARADA — VERIFIQUE O CABO / ADB"); else status.setText("● PC CONECTADO  |  DADOS AO VIVO");
        try {
            JSONObject j=new JSONObject(raw);
            cpu.setText("CPU\n"+val(j,"cpu_percent","%")+"\n"+val(j,"cpu_temp"," °C")+"\n"+val(j,"cpu_freq"," MHz"));
            gpu.setText("GPU\n"+val(j,"gpu_percent","%")+"\n"+val(j,"gpu_temp"," °C")+"\nVRAM "+val(j,"vram_used"," MB"));
            ram.setText("RAM\n"+val(j,"ram_percent","%")+"\n"+val(j,"ram_used"," GB")+" / "+val(j,"ram_total"," GB"));
            disk.setText("DISCO\n"+val(j,"disk_percent","%")+"\nLivre "+val(j,"disk_free"," GB"));
            temps.setText("TEMPERATURAS\nCPU: "+val(j,"cpu_temp"," °C")+"     GPU: "+val(j,"gpu_temp"," °C")+"\nPlaca-mãe: "+val(j,"board_temp"," °C")+"     SSD: "+val(j,"disk_temp"," °C")+"\nVentoinhas: "+j.optString("fans","Não disponíveis"));
            net.setText("REDE / INTERNET\nDOWNLOAD: "+val(j,"download_mbps"," Mbps")+"  ("+val(j,"download_mbs"," MB/s")+")\nUPLOAD: "+val(j,"upload_mbps"," Mbps")+"  ("+val(j,"upload_mbs"," MB/s")+")");
            updated.setText("Último pacote: "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date(received))+"   |   Sensores extras dependem do hardware/driver.");
        } catch(Exception e) { status.setText("● ERRO AO LER DADOS RECEBIDOS"); }
    }
    @Override protected void onDestroy(){ handler.removeCallbacks(refresh); super.onDestroy(); }
}
