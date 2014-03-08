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
      context.startService(msg);
   }

   private static void passExtra(String key, Intent intent, Intent msg)
   {
      if ( intent.hasExtra(key) )
         msg.putExtra(key, intent.getStringExtra(key));
   }
}
