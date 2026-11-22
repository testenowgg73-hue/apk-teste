package com.ultron.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;

public class StatsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !"com.ultron.monitor.UPDATE".equals(intent.getAction())) return;
        String data = intent.getStringExtra("payload_b64");
        if (data == null) return;
        try {
            String json = new String(Base64.decode(data, Base64.DEFAULT), "UTF-8");
            SharedPreferences p = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
            p.edit().putString("json", json).putLong("received", System.currentTimeMillis()).apply();
        } catch (Exception ignored) { }
    }
}
