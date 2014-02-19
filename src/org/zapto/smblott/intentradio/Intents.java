package org.zapto.smblott.intentradio;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

import org.zapto.smblott.intentradio.IntentPlayer;

public class Intents extends BroadcastReceiver {

   private static final String TAG = "IntentRadio";
   private static final String r4 = "http://www.bbc.co.uk/radio/listen/live/r4_heaacv2.pls";

   @Override
   public void onReceive(Context context, Intent intent) {
      String url = intent.hasExtra("url") ? intent.getStringExtra("url") : r4;
      Toast.makeText(context, url, Toast.LENGTH_SHORT).show();

      // Intent msg = new Intent(context, IntentPlayer.class);
      // msg.putExtra("action", intent.getAction());
      // if ( intent.hasExtra("url") )
      //    msg.putExtra("url", intent.getStringExtra("url"));
      // context.startService(msg);
   }

}
