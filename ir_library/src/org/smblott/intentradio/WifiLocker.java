package org.smblott.intentradio;

import android.content.Context;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiLocker extends WifiOn
{
   private static WifiLock lock = null;
   private static WifiManager manager = null;

   public static void lock(Context context, String app_name)
   {
      if ( manager == null )
         manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      if ( ! onWifi(context) )
         return;

      if ( lock == null )
         lock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL, app_name);

      if ( lock.isHeld() )
         return;

      log("Wifi lock: acquire");
      lock.acquire();
   }

   public static void unlock()
   {
      if ( lock != null && lock.isHeld() )
      {
         log("Wifi lock: release");
         lock.release();
      }
   }
}
