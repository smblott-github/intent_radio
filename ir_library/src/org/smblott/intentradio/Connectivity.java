package org.smblott.intentradio;

import android.content.Context;
import android.content.BroadcastReceiver;

import android.content.Intent;
import android.content.IntentFilter;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.AsyncTask;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public class Connectivity extends BroadcastReceiver
{
   private static ConnectivityManager connectivity = null;

   private Context context = null;
   private IntentPlayer player = null;
   private static final int TYPE_NONE = -1;

   Connectivity(Context a_context, IntentPlayer a_player)
   {
      Logger.log("Connectivity: created");
      context = a_context;
      player = a_player;

      init_connectivity(context);
      context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
   }

   static private void init_connectivity(Context context)
   {
      if ( connectivity == null )
         connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if ( connectivity != null )
         previous_type = getType();
   }

   void destroy()
      { context.unregisterReceiver(this); }

   static private int getType()
      { return getType(null); }

   static private int getType(Intent intent)
   {
      if (connectivity == null)
         return TYPE_NONE;

      if ( intent != null && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) )
         return TYPE_NONE;

      NetworkInfo network = connectivity.getActiveNetworkInfo();
      if ( network != null && network.isConnected() )
      {
         int type = network.getType();
         switch (type)
         {
            // These cases all fall through.
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_WIMAX:
               if ( network.getState() == NetworkInfo.State.CONNECTED )
                  return type;
         }
      }

      return TYPE_NONE;
   }

   static boolean onWifi()
      { return previous_type == ConnectivityManager.TYPE_WIFI; }

   static public boolean isConnected(Context context){
      init_connectivity(context);
      return (getType() != TYPE_NONE);
   }

   private static AsyncTask<Integer,Void,Void> disable_task = null;
   private static int previous_type = TYPE_NONE;
   private int then = 0;

   @Override
   public void onReceive(Context context, Intent intent)
   {
      int type = getType(intent);
      boolean want_network_playing = State.is_want_playing() && player.isNetworkUrl();
      Logger.log("Connectivity: " + type + " " + want_network_playing);

      if ( type == TYPE_NONE && previous_type != TYPE_NONE && want_network_playing )
      {  // We've lost connectivity.
         Logger.log("Connectivity: disconnected");
         player.stop();
         then = Counter.now();
         State.set_state(context, State.STATE_DISCONNECTED, true);

         if ( disable_task != null )
            disable_task.cancel(true);

         disable_task =
            new Later(300)
            {
               @Override
               public void later()
               {
                  player.stop();
                  disable_task = null;
               }
            }.start();
      }

      if ( previous_type == TYPE_NONE
            && type != previous_type
            && Counter.still(then)
            )
      {  // We have become reconnected, and we're still in the window to resume playback.
         Logger.log("Connectivity: connected");
         restart();
      }

      // We can get from mobile data to WiFi without going through TYPE_NONE.
      // So the counter does not help.
      // && Counter.still(then)
      if ( previous_type != TYPE_NONE && type != TYPE_NONE && type != previous_type && want_network_playing )
      {  // We have moved to a different type of network.
         Logger.log("Connectivity: different network type");
         restart();
      }

      previous_type = type;
      return;
   }

   private void restart()
   {
      if ( disable_task != null )
      {
         disable_task.cancel(true);
         disable_task = null;
      }

      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      if ( settings.getBoolean("reconnect", false) )
         player.play();
   }
}

