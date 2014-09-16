package org.smblott.intentradio;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class Intents extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      Intent msg = new Intent(context, IntentPlayer.class);
      msg.putExtra("action", intent.getAction());
      passExtra("url", intent, msg);
      passExtra("name", intent, msg);
      passExtra("debug", intent, msg);
      msg.putExtra("broadcast", true);
      context.startService(msg);
   }

   private static void passExtra(String key, Intent intent, Intent msg)
   {
      if ( intent.hasExtra(key) )
      {
         String str = intent.getStringExtra(key);
         if ( str != null )
            msg.putExtra(key, str);
      }
   }
}
