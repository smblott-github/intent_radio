package org.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
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
import android.media.MediaPlayer.OnCompletionListener;

import android.net.Uri;
import android.os.Build.VERSION;
import android.webkit.URLUtil;

public class IntentPlayer extends Service
   implements
      OnBufferingUpdateListener,
      OnInfoListener,
      OnErrorListener,
      OnPreparedListener,
      OnAudioFocusChangeListener,
      OnCompletionListener
{

   /* ********************************************************************
    * Globals...
    */

   private static final int note_id = 100;
   private static final String preference_file = "state";
   private static SharedPreferences settings = null;

   private static Context context = null;
   private static PendingIntent pending_stop = null;
   private static PendingIntent pending_play = null;
   private static PendingIntent pending_restart = null;

   private static String app_name = null;
   private static String app_name_long = null;
   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;
   private static String intent_state_request = null;

   private static String default_url = null;
   private static String default_name = null;
   public  static String name = null;
   public  static String url = null;

   private static NotificationManager note_manager = null;
   private static Notification note = null;
   private static MediaPlayer player = null;
   private static Builder builder = null;
   private static AudioManager audio_manager = null;

   private static Playlist playlist_task = null;
   private static AsyncTask<Integer,Void,Void> pause_task = null;

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
      intent_state_request = context.getString(R.string.intent_state_request);
      default_url = getString(R.string.default_url);
      default_name = getString(R.string.default_name);

      settings = getSharedPreferences(preference_file, context.MODE_PRIVATE);
      url = settings.getString("url", default_url);
      name = settings.getString("name", default_name);

      note_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      pending_stop = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);
      pending_play = PendingIntent.getBroadcast(context, 0, new Intent(intent_play), 0);
      pending_restart = PendingIntent.getBroadcast(context, 0, new Intent(intent_restart), 0);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

      builder =
         new Notification.Builder(context)
            .setSmallIcon(R.drawable.intent_radio)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(pending_stop)
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
      log("Destroyed.");
      stop();

      if ( player != null )
      {
         player.release();
         player = null;
      }

      Logger.state("off");
      super.onDestroy();
   }

   /* ********************************************************************
    * Main entry point...
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

      if ( action.equals(intent_state_request) )
      {
         State.get_state(context);
         return done();
      }

      if ( action.equals(intent_play) )
      {
         if ( intent.hasExtra("url") )
            url = intent.getStringExtra("url");

         if ( intent.hasExtra("name") )
            name = intent.getStringExtra("name");

         Editor editor = settings.edit();
         editor.putString("url", url);
         editor.putString("name", name);
         editor.commit();

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

   private int play()
      { return play(url); }

   private int play(String url)
   {
      stop(false);

      toast(name);
      log("Play: ", url);

      // /////////////////////////////////////////////////////////////////
      // Notification...

      builder
         .setOngoing(true)
         .setContentIntent(pending_stop)
         .setContentText("Connecting...");
      note = builder.build();

      startForeground(note_id, note);

      // /////////////////////////////////////////////////////////////////
      // Check URL...

      if ( ! URLUtil.isValidUrl(url) )
      {
         toast("Invalid URL.");
         return stop("Invalid URL.");
      }

      // /////////////////////////////////////////////////////////////////
      // Audio focus...

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return stop("Failed to obtain audio focus!");

      // /////////////////////////////////////////////////////////////////
      // Set up media player...

      if ( player == null )
      {
         log("Creating media player...");
         player = new MediaPlayer();
         player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
         player.setAudioStreamType(AudioManager.STREAM_MUSIC);

         player.setOnPreparedListener(this);
         player.setOnBufferingUpdateListener(this);
         player.setOnInfoListener(this);
         player.setOnErrorListener(this);
         player.setOnCompletionListener(this);
      }
      else
         log("Re-using existing player.");

      WifiLocker.lock(context, app_name_long);
      log("Connecting...");

      // /////////////////////////////////////////////////////////////////
      // Playlists...

      /* Is there a better way to test whether a URL is a playlist?
       */

      playlist_task = null;

      if ( url.endsWith(PlaylistPls.suffix) )
         playlist_task = new PlaylistPls(this);

      if ( url.endsWith(PlaylistM3u.suffix) )
         playlist_task = new PlaylistM3u(this);

      if ( playlist_task != null )
      {
         playlist_task.execute(url);
         notificate("Fetching playlist...");
         notificate_click_to_stop();
         return done(State.STATE_BUFFER);
      }

      // /////////////////////////////////////////////////////////////////
      // Ready...

      return play_launch(url);
   }

   /* ********************************************************************
    * Launch player...
    */

   private static String previous_launch_url = null;
   private static boolean previous_launch_successful = false;

   public int play_launch()
      { return play_launch(previous_launch_url); }

   public int play_launch(String url)
   {
      log("Launching: ", url);
      notificate("Connecting...");
      notificate_click_to_stop();

      if ( ! URLUtil.isValidUrl(url) )
      {
         previous_launch_url = null;
         previous_launch_successful = false;
         toast("Invalid URL.");
         return stop("Invalid URL.");
      }

      previous_launch_url = url;
      previous_launch_successful = false;

      try
      {
         player.setVolume(1.0f, 1.0f);
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
      }
      catch (Exception e)
         { return stop("Initialisation error."); }

      return done(State.STATE_BUFFER);
   }

   /* ********************************************************************
    * Stop...
    */

   private int stop()
   {
      // Stop, kill notification an send state.
      // This is a real and final stop().
      //
      return stop(true,null,true);
   }

   private int stop(boolean real_stop)
   {
      // Stop, kill notification and possibly send state.
      //
      return stop(true,null,real_stop);
   }

   private int stop(String msg)
   {
      // Stop, keep notification and do send state.
      //
      return stop(false,msg,true);
   }

   private int stop(boolean kill_note, String text, boolean real_stop)
   {
      log("Stopping kill_note: ", ""+kill_note);
      log("Stopping real_stop: ", ""+real_stop);
      log("Stopping text: ", text == null ? "null" : text);

      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      // Time moves on...
      //
      Counter.time_passes();
      previous_launch_url = null;

      // Stop player...
      //
      if ( player != null )
      {
         log("Stopping player...");
         if ( player.isPlaying() )
            player.stop();
         player.reset();
      }

      // Handle notification...
      //
      if ( kill_note || text == null || text.length() == 0 )
      {
         stopForeground(true);
         note = null;
      }
      else
      {
         log("Keeping (now-)dismissable note: ", text);
         notificate(text,false);
         notificate_click_to_restart();
      }

      if ( real_stop )
         // We're still holding resources, including the player itself.
         // Spin off a task to clean up, soon.
         //
         // No need to cancel this.  Because the state is now STATE_STOP, all
         // events effecting the relevance of this thread move time on.
         // 
         new Later()
         {
            @Override
            public void later()
            {
               if ( player != null )
               {
                  log("Releasing player.");
                  player.release();
                  player = null;
               }
            }
         }.start();

      return done(real_stop ? State.STATE_STOP : null);
   }

   /* ********************************************************************
    * Pause/restart...
    */

   private int pause()
      { return pause("Paused..."); }

   private int pause(String msg)
   {
      log("Pause: ", State.current());

      if ( player == null )
         return done();

      // if ( ! player.isPlaying() )
      //    return done();

      if ( State.is(State.STATE_PAUSE) || ! State.is_playing() )
         return done();

      if ( pause_task != null )
         pause_task.cancel(true);
      pause_task = null;

      if ( URLUtil.isNetworkUrl(previous_launch_url) )
         // We're still holding resources, including a Wifi Wakelock and the player
         // itself.  Spin off a task to convert this "pause" into a stop, soon, if
         // necessary.  This will be cacelled if we restart(), or become
         // irrelevant if another action such as stop() or play() occurs, because
         // then time will have passed.
         //
         pause_task =
            new Later()
            {
               @Override
               public void later()
               {
                  pause_task = null;
                  stop("Suspended, click to restart...");
               }
            }.start();

      player.pause();
      notificate(msg);
      notificate_click_to_restart();
      return done(State.STATE_PAUSE);
   }

   private int duck(String msg)
   {
      log("Duck: ", State.current());

      if ( State.is(State.STATE_DUCK) || ! State.is_playing() )
         return done();

      player.setVolume(0.1f, 0.1f);
      notificate(msg);
      notificate_click_to_stop();
      return done(State.STATE_DUCK);
   }

   private int restart()
   {
      log("Restart: ", State.current());

      if ( player == null || State.is(State.STATE_STOP) || State.is(State.STATE_ERROR) )
         return play();

      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
         return done();

      if ( State.is(State.STATE_DUCK) )
      {
         player.setVolume(0.1f, 0.1f);
         notificate();
         notificate_click_to_stop();
         return done(State.STATE_PLAY);
      }

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         toast("Intent Radio:\nFailed to (re-)acquire audio focus.");
         return done();
      }

      if ( pause_task != null )
      {
         pause_task.cancel(true);
         pause_task = null;
      }

      player.setVolume(1.0f, 1.0f);
      player.start();
      notificate();
      notificate_click_to_stop();
      return done(State.STATE_PLAY);
   }

   /* ********************************************************************
    * All onStartCommand() invocations end here...
    */

   private int done(String state)
   {
      if ( state != null )
         State.set_state(context, state);
      return done();
   }

   private int done()
   {
      return START_NOT_STICKY;
   }

   /* ********************************************************************
    * Listeners...
    */

   @Override
   public void onPrepared(MediaPlayer mp)
   {
      if ( mp == player )
      {
         log("Starting....");
         player.start();
         State.set_state(context, State.STATE_PLAY);
         notificate();
         notificate_click_to_stop();

         // A launch is successful if there is no error within the first few
         // seconds.  If a launch is successful then later the stream fails,
         // then the launch will be repeated.  If it fails before it is
         // considered successful, then it will not be repeated.  This is
         // intented to prevent thrashing.
         //
         // No need to cancel this.  All events effecting the relevance of this
         // thread move time on.
         // 
         new Later(20)
         {
            @Override
            public void later()
            {
               log("Launch successful.");
               previous_launch_successful = true;
            }
         }.start();
      }
   }

   @Override
   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      /*
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: ", ""+percent, "%"); 
         */
   }

   @Override
   public boolean onInfo(MediaPlayer player, int what, int extra)
   {
      switch (what)
      {
         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            State.set_state(context, State.STATE_BUFFER);
            notificate("Buffering...");
            notificate_click_to_stop();
            break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            State.set_state(context, State.STATE_PLAY);
            notificate();
            notificate_click_to_stop();
            break;
      }
      return true;
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra)
   {
      log("Error: ", ""+what);

      if ( player != null
            && previous_launch_url != null
            && previous_launch_successful
            && URLUtil.isNetworkUrl(previous_launch_url))
      {
         player.reset();
         play_launch();
      }
      else
      {
         switch ( what )
         {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
               stop("Media server died.");
               break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
               stop("Media error.");
               break;
            default:
               stop("Unknown error.");
               break;
         }
      }

      return true;
   }

   /* ********************************************************************
    * On completion listener...
    */

   @Override
   public void onCompletion(MediaPlayer mp)
   {
      log("Completion.");

      stop("Completed. Click to restart.");
      done(State.STATE_COMPLETE);

      // if ( player != null
      //       && previous_launch_url != null
      //       && previous_launch_successful
      //       && URLUtil.isNetworkUrl(previous_launch_url))
      // {
      //    player.reset();
      //    play_launch();
      // }
      // else
      // {
      //    stop("Completed. Click to restart.");
      //    done(State.STATE_COMPLETE);
      // }
   }

   /* ********************************************************************
    * Audio focus listeners...
    */

   @Override
   public void onAudioFocusChange(int change)
   {
      log("onAudioFocusChange: ", ""+change);

      if ( player != null )
         switch (change)
         {
            case AudioManager.AUDIOFOCUS_GAIN:
               log("audiofocus_gain");
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS:
               log("audiofocus_loss");
               stop("Audio focus lost, stopped...");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               log("audiofocus_loss_transient");
               pause("Audio focus lost, paused...");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
               log("audiofocus_loss_transient_can_duck");
               duck("Audio focus lost, ducking...");
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
         log("Notificate: ", msg == null ? name : msg);
         note =
            builder
               .setOngoing(ongoing)
               .setContentText(msg == null ? name : msg)
               .build();
         note_manager.notify(note_id, note);
      }
   }

   private void notificate_click_to_stop()
   {
      note = builder.setContentIntent(pending_stop).build();
      note_manager.notify(note_id, note);
   }

   private void notificate_click_to_restart()
   {
      note = builder.setContentIntent(pending_restart).build();
      note_manager.notify(note_id, note);
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

   @Override
   public IBinder onBind(Intent intent)
      { return null; }
}
