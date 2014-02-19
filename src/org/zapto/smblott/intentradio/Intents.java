package org.zapto.smblott.intentradio;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

// import org.zapto.smblott.intentradio.IntentPlayer;

public class Intents extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      Intent msg = new Intent(context, IntentPlayer.class);
      msg.putExtra("action", intent.getAction());
      if ( intent.hasExtra("url") )
         msg.putExtra("url", intent.getStringExtra("url"));
      context.startService(msg);
   }

}
