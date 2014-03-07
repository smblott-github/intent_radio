package org.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;

import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;

import android.net.Uri;
import android.os.Build.VERSION;

public class IntentPlayer extends Service
   implements OnBufferingUpdateListener, OnInfoListener, OnErrorListener, OnPreparedListener
{

   /* ********************************************************************
    * Globals...
    */

   private static final boolean play_disabled = false;
   private static Context context = null;
   private static PendingIntent pending = null;

   private static final int note_id = 100;

   private static String app_name = null;
   private static String app_name_long = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   private static String name = null;
   private static String url = null;

   private static int counter = 0;
   private static Playlist pltask = null;
   private static MediaPlayer player = null;
   private static Builder builder = null;
   private static Notification note = null;
   private static NotificationManager note_manager = null;

   /* ********************************************************************
    * Create service...
    */

   @Override
   public void onCreate() {
      context = getApplicationContext();
      Logger.init(context);

      app_name = getString(R.string.app_name);
      app_name_long = getString(R.string.app_name_long);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);

      note_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      pending = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);

      builder =
         new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            // .setLargeIcon(BITMAP)
            .setPriority(Notification.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pending)
            .setContentTitle(app_name_long)
            // not available in API 16...
            // .setShowWhen(false)
            ;
   }

   /* ********************************************************************
    * Destroy service...
    */

   public void onDestroy()
   {
      stop();
      Logger.stop();
   }

   /* ********************************************************************
    * Primary entry point...
    *
    * For playlists, the fetching of the play list is handled asynchronously.
    * A second play intent is then delivered to onStartCommand.  To ensure that
    * that request is still valid, a counter is checked.  The extra cnt is the
    * value of counter at the time that the asynchronous call was launched.
    * That value must be unchanged when the susequent play intent is received.
    *
    * counter is incremented in stop(), which is called for every valid intent
    * which is received.
    */

   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      if ( intent != null && intent.hasExtra("debug") )
         Logger.start();

      if ( intent == null || ! intent.hasExtra("action") )
         return done();

      String action = intent.getStringExtra("action");
      if ( action == null )
         return done();
      log(action);

      if ( intent_stop.equals(action) )
         return stop();

      if ( intent_play.equals(action) && intent.hasExtra("cnt") )
      {
         // This is a play request subsequent to an asynchronous playlist
         // play request: validate cnt.
         int cnt = intent.getIntExtra("cnt",0);
         log("checking counter: " + cnt);
         if ( cnt != counter )
         {
            log("incorrect counter: counter=" + counter + " cnt=" + cnt);
            return done();
         }
      }

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

      log("unknown action: " + action);
      return done();
   }

   /* ********************************************************************
    * Play...
    */

   private int play(String url)
   {
      stop();

      if ( url == null )
      {
         toast("No URL.");
         return done();
      }

      if ( url.endsWith(PlaylistPls.suffix) )
      {
         log("playlist/pls: " + url);
         pltask = new PlaylistPls(context,intent_play);
         pltask.execute(url, name, ""+counter);
         return done();
      }

      toast(name);
      log("play: " + url);

      if ( play_disabled )
         return stop();

      builder.setContentText(name + ", connecting...");
      note = builder.build();

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
         toast("MediaPlayer: initialisation error.");
         toast(e.getMessage());
         return stop();
      }

      return done();
   }

   /* ********************************************************************
    * Stop...
    */

   private int stop()
   {
      counter += 1;

      if ( pltask != null )
      {
         pltask.cancel(true);
         pltask = null;
      }

      if ( player != null )
      {
         toast("Stopping...");
         stopForeground(true);
         player.stop();
         player.reset();
         player.release();
         player = null;
      }

      note = null;
      return done();
   }

   /* ********************************************************************
    * All onStartCommand invocations end here...
    */

   int done()
   {
      return START_NOT_STICKY;
   }

   /* ********************************************************************
    * Listeners...
    */

   public void onPrepared(MediaPlayer player)
   {
      log("Prepared, starting....");
      player.start();
      notificate();
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
      toast(msg);
      return true;
   }

   public boolean onError(MediaPlayer player, int what, int extra)
   {
      String msg = "onError...(" + what + ")";
      toast(msg);
      stop();
      return true;
   }

   /* ********************************************************************
    * Logging...
    */

   private void log(String msg)
      { Logger.log(msg); }

   private void toast(String msg)
      { Logger.toast(msg); }

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
