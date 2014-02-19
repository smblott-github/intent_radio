package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

public class IntentPlayer extends Service {

   private MediaPlayer player = null;
   private boolean playing = false;

   private static final String TAG = "IntentRadio";
   private static final String r4 = "http://www.bbc.co.uk/radio/listen/live/r4_heaacv2.pls";

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Context context = getApplicationContext();
      String action = intent.getStringExtra("action");
      Toast.makeText(context, "ABC " + action, Toast.LENGTH_SHORT).show();

      // String dataString = workIntent.getDataString();
      return Service.START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }
}
