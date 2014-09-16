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
      context = a_context;
      player = a_player;

      init_connectivity(context);
      context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
   }

   void destroy()
      { context.unregisterReceiver(this); }

   static private int getType()
   {
      if (connectivity != null)
      {
         NetworkInfo network = connectivity.getActiveNetworkInfo();
         if ( network != null && network.isConnected() )
         {
            int type = network.getType();
            switch (type)
            {
               case ConnectivityManager.TYPE_WIFI:
               case ConnectivityManager.TYPE_MOBILE:
               case ConnectivityManager.TYPE_WIMAX:
                  return type;
            }
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
      int type = getType();

      if ( State.is(State.STATE_STOP)
            || State.is(State.STATE_PAUSE)
            || State.is(State.STATE_COMPLETE)
            || ! player.isNetworkUrl() )
      {  // We have no involvement in these cases, so bail quickly.
         previous_type = type;
         return;
      }

      if ( previous_type == TYPE_NONE && type != previous_type && Counter.still(then) )
      {  // We have become connected, and we're still in the window to resume playback.
         if ( disable_task != null )
            { disable_task.cancel(true); disable_task = null; }

         player.play();
      }

      if ( previous_type != TYPE_NONE && type != TYPE_NONE && type != previous_type && Counter.still(then) )
      {  // We have moved to a different type of network, and we're still in the window to resume playback.
         if ( disable_task != null )
            { disable_task.cancel(true); disable_task = null; }

         player.play();
      }

      if ( type == TYPE_NONE && previous_type != type )
      {  // We've lost connectivity.
         player.stop();
         then = Counter.now();
         State.set_state(context, State.STATE_DISCONNECTED);

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

      previous_type = type;
      return;
   }
}
