package org.smblott.intentradio;

import android.content.Context;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Wifi
{
   private static WifiLock lock = null;
   private static WifiManager mgr = null;

   public static void lock(Context context, String app_name)
   {
      unlock();

      if ( mgr == null )
         mgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      if ( ! onWifi(context) )
         return;

      if ( lock == null )
         lock = mgr.createWifiLock(WifiManager.WIFI_MODE_FULL, app_name);

      if ( lock.isHeld() )
         return;

      log("Wifi: acquire()");
      lock.acquire();
   }

   public static void unlock()
   {
      if ( lock != null && lock.isHeld() )
      {
         log("Wifi: release()");
         lock.release();
      }
   }

   /* ********************************************************************
    * Utilities...
    */

   private static ConnectivityManager conn = null;

   private static boolean onWifi(Context context)
   {
      if ( conn == null )
         conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo info = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      boolean status = info.isAvailable();

      log("Wifi status: " + status);
      return status;
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String msg)
      { Logger.log(msg); }
}
