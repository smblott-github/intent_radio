package org.smblott.intentradio;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiOn
{
   private static ConnectivityManager conn = null;

   /* ********************************************************************
    * Is WiFi on?
    */

   protected static boolean onWifi(Context context)
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

   protected static void log(String msg)
      { Logger.log(msg); }
}
