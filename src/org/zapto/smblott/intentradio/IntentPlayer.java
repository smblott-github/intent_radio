package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

public class IntentPlayer extends Service {

   private MediaPlayer player = null;

   private static final String TAG = "IntentRadio";
   private static final String r4 = "http://bbcmedia.ic.llnwd.net/stream/bbcmedia_intl_he_radio4_p";
   // private static final String r4 = "http://www.bbc.co.uk/radio/listen/live/r4_heaacv2.pls";

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Context context = getApplicationContext();
      String action = intent.getStringExtra("action");
      if ( action == null )
         return Service.START_NOT_STICKY;
      String url = intent.hasExtra("url") ? intent.getStringExtra("url") : r4;

      if ( "org.zapto.smblott.intentradio.PLAY".equals(action) )
         play(context, url);

      if ( "org.zapto.smblott.intentradio.STOP".equals(action) )
         stop();

      return Service.START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

   void toast(String msg)
   {
      Log.d(TAG, msg);
      Context context = getApplicationContext();
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
   }

   void play(Context context, String url)
   {
      stop();
      player = new MediaPlayer();
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);
      try
      {
         toast(url);
         player.setDataSource(context, Uri.parse(url));
         player.setOnPreparedListener(
            new OnPreparedListener()
            {
               public void onPrepared(MediaPlayer player) {
                  player.start();
                  toast("Ok.");
               }
            }
         );
         player.prepareAsync();
         toast("Async.");
      }
      catch (Exception e)
      {
         toast("Error.");
      }
   }

   void stop()
   {
      if ( player != null )
      {
         toast("Stopping...");
         player.stop();
         player.reset();
         player.release();
         player = null;
      }
      else
         toast("Stopped.");
   }
}
