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
import android.media.AudioManager.OnAudioFocusChangeListener;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;

import android.net.Uri;
import android.os.Build.VERSION;

public class IntentPlayer extends Service
   implements
      OnBufferingUpdateListener,
      OnInfoListener,
      OnErrorListener,
      OnPreparedListener,
      OnAudioFocusChangeListener
{

   /* ********************************************************************
    * Globals...
    */

   private static final boolean play_disabled = false;
   private static final int note_id = 100;

   private static Context context = null;
   private static PendingIntent pending = null;

   private static String app_name = null;
   private static String app_name_long = null;
   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;

   private static String name = null;
   private static String url = null;

   private static Playlist pltask = null;
   private static MediaPlayer player = null;
   private static Builder builder = null;
   private static Notification note = null;
   private static NotificationManager note_manager = null;
   private static AudioManager audio_manager = null;

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
      intent_pause = getString(R.string.intent_pause);
      intent_restart = getString(R.string.intent_restart);

      note_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      pending = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

      builder =
         new Notification.Builder(context)
            .setSmallIcon(R.drawable.intent_radio)
            .setPriority(Notification.PRIORITY_HIGH)
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
      Logger.state("off");
   }

   /* ********************************************************************
    * Primary entry point...
    */

   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      if ( intent == null || ! intent.hasExtra("action") )
         return done();

      if ( intent.hasExtra("debug") )
         Logger.state(intent.getStringExtra("debug"));

      if ( ! Counter.still(intent.getIntExtra("counter", Counter.now())) )
         return done();

      String action = intent.getStringExtra("action");
      log("Action: ", action);

      if ( action.equals(intent_stop)    ) return stop();
      if ( action.equals(intent_pause)   ) return pause();
      if ( action.equals(intent_restart) ) return restart();

      if ( action.equals(intent_play) )
      {
         url = getString(R.string.default_url);
         name = getString(R.string.default_name);

         if ( intent.hasExtra("url") )
         {
            url = intent.getStringExtra("url");
            name = intent.hasExtra("name") ? intent.getStringExtra("name") : url;
         }
         else if ( intent.hasExtra("name") )
            name = intent.getStringExtra("name");

         log("Name: ", name);
         log("URL: ", url);
         return play(url);
      }

      log("unknown action: ", action);
      return done();
   }

   /* ********************************************************************
    * Play...
    */

   public void play(String url, int then)
   {
      if ( Counter.still(then) )
         play(url);
   }

   private int play(String url)
   {
      stop();

      if ( url == null )
         { toast("No URL."); return done(); }

      // /////////////////////////////////////////////////////////////////
      // Playlists...

      if ( url.endsWith(PlaylistPls.suffix) )
         pltask = new PlaylistPls(this);

      if ( url.endsWith(PlaylistM3u.suffix) )
         pltask = new PlaylistM3u(this);

      if ( pltask != null )
      {
         log("Playlist: ", url);
         pltask.execute(url);
         return done();
      }

      // /////////////////////////////////////////////////////////////////
      // Set up media player...

      log("Play: ", url);
      toast(name);

      if ( play_disabled )
         return stop();

      builder.setOngoing(true).setContentText("Connecting...");
      note = builder.build();

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return stop("Failed to get audio focus!");

      WifiLocker.lock(context, app_name_long);
      player = new MediaPlayer();
      player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);

      // /////////////////////////////////////////////////////////////////
      // Listeners...

      player.setOnPreparedListener(this);
      player.setOnBufferingUpdateListener(this);
      player.setOnInfoListener(this);
      player.setOnErrorListener(this);

      // /////////////////////////////////////////////////////////////////
      // Launch...

      startForeground(note_id, note);
      log("Connecting...");

      try
      {
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
      }
      catch (Exception e)
         { return stop("Initialisation error."); }

      return done();
   }

   /* ********************************************************************
    * Stop...
    */

   private int stop()
      { return stop(true,null); }

   private int stop(String msg)
      { return stop(false,msg); }

   private int stop(boolean kill_note, String text)
   {
      // Time moves on...
      //
      Counter.time_passes();

      // Kill any outstanding asynchronous playlist task...
      //
      if ( pltask != null )
      {
         pltask.cancel(true);
         pltask = null;
      }

      // Stop player...
      //
      if ( player != null )
      {
         log("Stopping player...");
         player.stop();
         player.reset();
         player.release();
         player = null;
         WifiLocker.unlock();
      }

      // Kill or keep notification...
      //
      stopForeground(true);

      if ( kill_note || text == null || text.length() == 0 )
         note = null;
      else
         notificate(text,false);

      return done();
   }

   /* ********************************************************************
    * Pause/restart...
    */

   private int pause()
   {
      Counter.time_passes();

      if ( player != null && player.isPlaying() )
      {
         player.pause();
         notificate("Paused.");
      }

      return done();
   }

   private int restart()
   {
      Counter.time_passes();

      if ( player != null && ! player.isPlaying() )
      {
         player.start();
         notificate();
      }

      return done();
   }

   /* ********************************************************************
    * All onStartCommand invocations end here...
    */

   int done()
      { return START_NOT_STICKY; }

   /* ********************************************************************
    * Listeners...
    */

   public void onPrepared(MediaPlayer a_player)
   {
      if ( a_player == player )
      {
         log("Prepared, starting....");
         player.start();
         notificate();
      }
   }

   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: ", ""+percent, "%"); 
   }

   public boolean onInfo(MediaPlayer player, int what, int extra)
   {
      log("onInfo: ", ""+what);
      String msg = "Buffering: " + what;
      switch (what)
      {
         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            log(msg, "/end");
            notificate();
            return true;

         // not available in API 16...
         // case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
         //    msg += "/media unsupported"; break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            msg += "/start..."; break;
         case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            msg += "/bad interleaving"; break;
         case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            msg += "/media not seekable"; break;
         case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            msg += "/media info update"; break;
         default:
            return true;
      }
      notificate(msg);
      toast(msg);
      return true;
   }

   public boolean onError(MediaPlayer player, int what, int extra)
   {
      String msg = "onError...(" + what + ")";
      toast(msg);
      stop("Error: " + what + ".");
      return true;
   }

   /* ********************************************************************
    * Audio focus listeners...
    */

   public void onAudioFocusChange(int change)
   {
      Counter.time_passes();
      log("onAudioFocusChange: ", ""+change);

      if ( player != null )
         switch (change)
         {
            case AudioManager.AUDIOFOCUS_GAIN:
               log("Audio focus: AUDIOFOCUS_GAIN");
               restart();
               player.setVolume(1.0f, 1.0f);
               notificate();
               break;

            case AudioManager.AUDIOFOCUS_LOSS:
               log("Audio focus: AUDIOFOCUS_LOSS");
               stop("Audio focus lost, streaming stopped.");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               log("Audio focus: AUDIOFOCUS_LOSS_TRANSIENT");
               pause();
               notificate("Focus lost, paused...");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
               log("Audio focus: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
               player.setVolume(0.1f, 0.1f);
               notificate("Focus lost, quiet mode...");
               break;

            default:
               log("Audio focus: unhandled");
               break;
      }
   }

   /* ****************************************************************
    * Notifications...
    */

   private void notificate()
      { notificate(null); }

   private void notificate(String msg)
      { notificate(msg,true); }

   private void notificate(String msg, boolean ongoing)
   {
      if ( note != null )
      {
         note =
            builder
               .setOngoing(ongoing)
               .setContentText(msg == null ? name : msg)
               .build();
         note_manager.notify(note_id, note);
      }
   }

   /* ********************************************************************
    * Logging...
    */

   private void log(String... msg)
      { Logger.log(msg); }

   private void toast(String msg)
      { Logger.toast(msg); }

   /* ********************************************************************
    * Required abstract method...
    */

   public IBinder onBind(Intent intent)
      { return null; }
}
