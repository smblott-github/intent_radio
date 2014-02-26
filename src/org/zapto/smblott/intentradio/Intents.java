package org.zapto.smblott.intentradio;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class Intents extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      Intent msg = new Intent(context, IntentPlayer.class);
      msg.putExtra("action", intent.getAction());
      if ( intent.hasExtra("url")  ) msg.putExtra("url",  intent.getStringExtra("url") );
      if ( intent.hasExtra("name") ) msg.putExtra("name", intent.getStringExtra("name"));
      context.startService(msg);
   }

}
