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
   private int then = 0;
   private static AsyncTask<Integer,Void,Void> disable_task = null;

   Connectivity(Context a_context, IntentPlayer a_player)
   {
      context = a_context;
      player = a_player;

      init_connectivity(context);
      context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
   }

   void destroy()
      { context.unregisterReceiver(this); }

   static private void init_connectivity(Context context)
   {
      if ( connectivity == null )
         connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
   }

   static public boolean isConnected(Context context){
      init_connectivity(context);

      if (connectivity == null) 
          return false;

      NetworkInfo[] info = connectivity.getAllNetworkInfo();

      /* Do we really have to ask for all state and check each item individually? */
      if (info != null) 
         for (int i = 0; i < info.length; i+=1) 
            if (info[i].getState() == NetworkInfo.State.CONNECTED)
               return true;

       return false;
   }

   @Override
   public void onReceive(Context context, Intent intent)
   {
      // This is called every time the network state changes.  So we need to
      // bail out quickly if it's not relevant to us.  Ideally, we would
      // register and deregister the listener as we go along.
      //
      if ( State.is_stopped() || State.is(State.STATE_PAUSE) )
         return;

      // Logger.log("Network state change, connected = " + isConnected(context));

      // ///////////////////////////////////////////////////
      // We seem to have lost our internet connection....
      //
      // Note: the following is not triggered if we are paused.
      //
      if ( State.is_playing() && ! State.is(State.STATE_DISCONNECTED) && ! isConnected(context) )
      {
         Logger.log("Disconnected...");

         player.stop();
         then = Counter.now();
         State.set_state(context, State.STATE_DISCONNECTED);

         if ( disable_task != null )
            disable_task.cancel(true);

         // If we're not reconnected within five minutes, then do not try to
         // reconnect.
         //
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

         return;
      }

      // ///////////////////////////////////////////////////
      // We seem to have been reconnected....
      //
      if ( State.is(State.STATE_DISCONNECTED) && Counter.still(then) && isConnected(context) )
      {
         Logger.log("Reconnected...");

         SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
         if ( ! settings.getBoolean("reconnect", false) )
            return;

         if ( disable_task != null )
         {
            disable_task.cancel(true);
            disable_task = null;
         }

         player.play();
         return;
      }

   }
}
