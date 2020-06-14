package com.runnersudl.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import static com.runnersudl.rutas.RutasFragment.checkNetwork;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            String type = networkType(context);
            switch (type) {
                case "WiFi":
                    checkNetwork("WiFi");
                    Log.e("---", "Online Connect WiFi");
                    break;
                case "Mobl":
                    checkNetwork("Mobl");
                    Log.e("---", "Online Connect Data");
                    break;
                default:
                    checkNetwork("");
                    Log.e("---", "Conectivity Failure !!! ");
                    break;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private String networkType(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return "WiFi";
            } else if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "Mobl";
            } else {
                return "None";
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "";
        }
    }
}
