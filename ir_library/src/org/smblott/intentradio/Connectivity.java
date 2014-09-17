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

   void destroy()
      { context.unregisterReceiver(this); }

   static private int getType()
      { return getType(null); }

   static private int getType(Intent intent)
   {
      if (connectivity == null)
         return TYPE_NONE;

      if ( intent != null )
      {
         if ( intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) )
            return TYPE_NONE;

      }

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

   static private void init_connectivity(Context context)
   {
      if ( connectivity == null )
      {
         connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
         previous_type = getType();
      }
   }

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
      Logger.log("Connectivity: " + type);

      /*
      if ( State.is(State.STATE_PAUSE)
            || State.is(State.STATE_COMPLETE)
            || ! player.isNetworkUrl() )
            */
      // if ( ! player.isNetworkUrl() )
      // {  // We have no involvement in these cases, so bail quickly.
      //    Logger.log("Connectivity: bailing");
      //    if ( disable_task != null )
      //       { disable_task.cancel(true); disable_task = null; }
      //    previous_type = type;
      //    return;
      // }

      boolean network_playing = (State.is(State.STATE_PLAY) || State.is(State.STATE_ERROR)) && player.isNetworkUrl();

      if ( type == TYPE_NONE && previous_type != TYPE_NONE && network_playing )
      {  // We've lost connectivity.
         Logger.log("Connectivity: disconnected");
         player.stop();
         then = Counter.now();
         State.set_state(context, State.STATE_DISCONNECTED, true);

         if ( disable_task != null )
            disable_task.cancel(true);

         disable_task =
            // 300 seconds is enough time to pop into the supermarket, perhaps?
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
      {  // We have become connected, and we're still in the window to resume playback.
         Logger.log("Connectivity: connected");
         if ( disable_task != null )
            { disable_task.cancel(true); disable_task = null; }

         player.play();
      }

      // We can get from mobile data to WiFi without going through TYPE_NONE.
      // && Counter.still(then)
      if ( previous_type != TYPE_NONE && type != TYPE_NONE && type != previous_type && network_playing )
      {  // We have moved to a different type of network, and we're still in the window to resume playback.
         Logger.log("Connectivity: different network type");
         if ( disable_task != null )
            { disable_task.cancel(true); disable_task = null; }

         player.play();
      }

      previous_type = type;
      return;
   }
}
