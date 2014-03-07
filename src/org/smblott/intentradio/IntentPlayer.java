package org.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.AsyncTask;

import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;

import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnErrorListener;

import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

public class IntentPlayer extends Service
   implements OnBufferingUpdateListener, OnInfoListener, OnErrorListener, OnPreparedListener
{

   /* ********************************************************************
    * Globals...
    */

   private static final boolean debug_logcat = true;
   private static final boolean debug_file = true;
   private static final boolean play_disabled = false;
   private static Context context = null;
   private static PendingIntent pend_intent = null;

   private static final int note_id = 100;

   private static String app_name = null;
   private static String app_name_long = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   private static FileOutputStream log_file_stream = null;

   private static volatile String name = null;
   private static volatile String url = null;

   private static volatile AsyncTask<String, Void, Void> atask = null;
   private static volatile MediaPlayer player = null;
   private static volatile Builder builder = null;
   private static volatile Notification note = null;
   private static volatile NotificationManager note_manager = null;

   /* ********************************************************************
    * Create/destroy...
    */

   @Override
   public void onCreate() {
      context = getApplicationContext();

      // Remove once playlist requests are correctly handled in a separate thread...
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);

      app_name = getString(R.string.app_name);
      app_name_long = getString(R.string.app_name_long);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);

      note_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      pend_intent = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);

      if ( debug_file )
         try
         {
            File log_file = new File(getExternalFilesDir(null), getString(R.string.intent_log_file));
            log_file_stream = new FileOutputStream(log_file);
         }
         catch (Exception e) { log_file_stream = null; }

      builder =
         new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(Notification.PRIORITY_HIGH)
            .setOngoing(true)
            // not available in API 16...
            // .setShowWhen(false)
            .setContentIntent(pend_intent)
            ;
   }

   public void onDestroy()
   {
      stop();
      if ( log_file_stream != null )
      {
         try { log_file_stream.close(); }
         catch (Exception e) { }
         log_file_stream = null;
      }
   }

   /* ********************************************************************
    * Primary entry point...
    */

   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      if ( intent == null )
         return stop();

      String action = intent.getStringExtra("action");
      if ( action == null )
         return stop();
      log(action);

      if ( intent_stop.equals(action) )
         return stop();

      if ( intent_play.equals(action) )
      {
         url = intent.hasExtra("url") ? intent.getStringExtra("url") : getString(R.string.default_url);
         name = intent.hasExtra("name") ? intent.getStringExtra("name") : url;

         if ( ! intent.hasExtra("url") && ! intent.hasExtra("name") )
            name = getString(R.string.default_name);

         log(name);
         log(url);
         return play(url);
      }

      return stop();
   }

   /* ********************************************************************
    * PlaylistPlsGetter...
    */

   private class PlaylistPlsGetter extends AsyncTask<String, Void, Void>
   {
      protected Void doInBackground(String... args)
      {
         if ( args.length != 2 )
         {
            log("PlaylistPlsGetter: invalid args length");
            return null;
         }

         String plsUrl = args[0];
         String nm = args[1];

         if ( plsUrl == null )
         {
            log("PlaylistPlsGetter: no playlist url");
            return null;
         }

         if ( nm == null )
         {
            log("PlaylistPlsGetter: no name");
            return null;
         }

         String url = PlaylistPls.get(plsUrl);

         if ( url == null )
         {
            log("PlaylistPlsGetter: failed to extract url");
            return null;
         }

         if ( url.endsWith(".pls") )
         {
            log("PlaylistPlsGetter: another paylist!");
            return null;
         }

         Intent msg = new Intent(context, IntentPlayer.class);
         msg.putExtra("action", intent_play);
         msg.putExtra("url", url);
         msg.putExtra("name", nm);

         if ( ! isCancelled() )
            context.startService(msg);

         return null;
      }
   }

   /* ********************************************************************
    * Play...
    */

   private int play(String url)
   {
      stop();

      builder.setContentTitle(app_name_long);
      builder.setContentText(name + ": connecting...");
      note = builder.build();

      if ( url != null && url.endsWith(".pls") )
      {
         log("playlist/pls: " + url);
         atask = new PlaylistPlsGetter();
         atask.execute(url, name);
         return Service.START_NOT_STICKY;
      }

      if ( url != null )
      {
         toast(name);
         log("play: " + url);
         return play_start(url);
      }

      toast("No URL.", true);
      return stop();
   }

   private int play_start(String url)
   {
      log("play_start: " + url);

      if ( play_disabled )
         return stop();

      player = new MediaPlayer();
      player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);

      player.setOnPreparedListener(this);
      player.setOnBufferingUpdateListener(this);
      player.setOnInfoListener(this);
      player.setOnErrorListener(this);

      try
      {
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
         startForeground(note_id, note);
         log("Connecting...");
      }
      catch (Exception e)
      {
         toast("MediaPlayer: initialisation error.", true);
         toast(e.getMessage(), true);
         return stop();
      }

      return Service.START_NOT_STICKY;
   }

   /* ********************************************************************
    * Stop...
    */

   private int stop()
   {
      if ( atask != null )
      {
         atask.cancel(true);
         atask = null;
      }

      if ( player != null )
      {
         toast("Stopping...", true);
         stopForeground(true);
         player.stop();
         player.reset();
         player.release();
         player = null;
      }

      note = null;
      return Service.START_NOT_STICKY;
   }

   /* ********************************************************************
    * Listeners...
    */

   public void onPrepared(MediaPlayer player) {
      player.start();
      notificate();
      log("Ok (onPrepared).");
   }

   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: " + percent + "%"); 
   }

   public boolean onInfo(MediaPlayer player, int what, int extra)
   {
      String msg = "" + what;
      switch (what)
      {
         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            notificate(); return true;

         // not available in API 16...
         // case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
         //    msg += "/media unsupported"; break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            msg += "/buffering start.."; break;
         case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            msg += "/bad interleaving"; break;
         case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            msg += "/media not seekable"; break;
         case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            msg += "/media info update"; break;
      }
      notificate(msg);
      toast(msg, true);
      return true;
   }

   public boolean onError(MediaPlayer player, int what, int extra)
   {
      String msg = "onError...(" + what + ")";
      toast(msg, true);
      stop();
      return true;
   }

   /* ********************************************************************
    * Logging...
    */

   private void log_to_file(String msg)
   {
      if ( log_file_stream == null || msg == null )
         return;

      DateFormat format = new SimpleDateFormat("HH:mm:ss ");
      String stamp = format.format(new Date());

      try
      {
         log_file_stream.write((stamp+msg+"\n").getBytes());
         log_file_stream.flush();
      } catch (Exception e) {}
   }

   private void log(String msg)
   {
      if ( msg == null )
         return;

      log_to_file(msg);
      if ( debug_logcat )
         Log.d(app_name, msg);
   }

   private void toast(String msg, boolean log_too)
   {
      if ( msg == null )
         return;

      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
      if ( log_too )
         log(msg);
   }

   private void toast(String msg)
   {
      toast(msg,false);
   }

   /* ****************************************************************
    * Notifications...
    */

   private void notificate()
   {
      notificate(null);
   }

   private void notificate(String msg)
   {
      if ( note != null )
      {
         builder.setContentText(msg == null ? name : msg);
         note = builder.build();
         note_manager.notify(note_id, note);
      }
   }

   /* ********************************************************************
    * Required abstract method...
    */

   public IBinder onBind(Intent intent) {
      return null;
   }

}
