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

   private static String app_name = null;
   private static String app_name_long = null;
   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;
   private static String intent_state_request = null;
   private static String intent_click = null;

   private static String default_url = null;
   private static String default_name = null;
   public  static String name = null;
   public  static String url = null;

   private static MediaPlayer player = null;
   private static AudioManager audio_manager = null;

   private static Playlist playlist_task = null;
   private static AsyncTask<Integer,Void,Void> pause_task = null;

   private static Connectivity connectivity = null;

   /* ********************************************************************
    * Create service...
    */

   @Override
   public void onCreate() {
      context = getApplicationContext();
      Logger.init(context);
      Notify.init(this,context);

      app_name = getString(R.string.app_name);
      app_name_long = getString(R.string.app_name_long);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      intent_pause = getString(R.string.intent_pause);
      intent_restart = getString(R.string.intent_restart);
      intent_state_request = context.getString(R.string.intent_state_request);
      intent_click = getString(R.string.intent_click);
      default_url = getString(R.string.default_url);
      default_name = getString(R.string.default_name);

      settings = getSharedPreferences(preference_file, context.MODE_PRIVATE);
      url = settings.getString("url", default_url);
      name = settings.getString("name", default_name);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      connectivity = new Connectivity(context,this);
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

      if ( connectivity != null )
      {
         connectivity.destroy();
         connectivity = null;
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
      if ( action.equals(intent_click)   ) return click();

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
         Notify.name(name);
         return play(url);
      }

      log("unknown action: ", action);
      return done();
   }

   /* ********************************************************************
    * Play...
    */

   public int play()
      { return play(url); }

   private int play(String url)
   {
      stop(false);

      toast(name);
      log("Play: ", url);

      // /////////////////////////////////////////////////////////////////
      // Check URL...

      if ( ! URLUtil.isValidUrl(url) )
      {
         toast("Invalid URL.");
         return stop();
      }

      // /////////////////////////////////////////////////////////////////
      // Check connectivity...

      if ( ! Connectivity.isConnected(context) )
      {
         int ret = stop();
         State.set_state(context, State.STATE_DISCONNECTED, isNetworkUrl());
         toast("No internet connection; will not start playback.");
         return ret;
      }

      // /////////////////////////////////////////////////////////////////
      // Audio focus...

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         toast("Could not obtain audio focus; not playing.");
         return stop();
      }

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

      // All requests (including non-playlists), now go through the Playlist
      // class.  This simplifies the logic of deciding what is what.
      //
      playlist_task = new Playlist(this,url);
      playlist_task.start();

      return done(State.STATE_BUFFER);
   }

   public boolean isNetworkUrl()
   {
      return ( previous_launch_url != null && URLUtil.isNetworkUrl(previous_launch_url) );
   }

   /* ********************************************************************
    * Launch player...
    */

   private static String previous_launch_url = null;
   private static boolean previous_launch_successful = false;

   public void play_relaunch()
   {
      if ( previous_launch_successful && previous_launch_url != null )
         play_launch(previous_launch_url);
   }

   public int play_launch(String url)
   {
      log("Launching: ", url);

      previous_launch_url = null;
      previous_launch_successful = false;

      if ( ! URLUtil.isValidUrl(url) )
      {
         toast("Invalid URL.");
         return stop();
      }

      previous_launch_url = url;

      try
      {
         player.setVolume(1.0f, 1.0f);
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
      }
      catch (Exception e)
         { return stop(); }

      // The following is not working.
      // new Metadata(context,url).start();
      return done(State.STATE_BUFFER);
   }

   /* ********************************************************************
    * Stop...
    */

   public int stop()
      { return stop(true); }

   // Parameters:
   //   text: text to put in notification (if it is not dismissed).
   //   real_stop: usually true; only false when stop() is called from play();
   //              that is, when we're about to start playback, and we are only
   //              stopping to clean up state and move time on.
   //
   private int stop(boolean real_stop)
   {
      log("Stopping real_stop: ", ""+real_stop);

      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      // Time moves on...
      //
      Counter.time_passes();
      previous_launch_url = null;
      previous_launch_successful = false;

      // Stop player...
      //
      if ( player != null )
      {
         log("Stopping player...");
         if ( player.isPlaying() )
            player.stop();
         log("releasing player...");
         // If the player is still connecting, then resetting or releasing the
         // player hangs for quite some time.
         // 
         player.release();
         player = null;
      }

      if ( playlist_task != null )
      {
         playlist_task.cancel(true);
         playlist_task = null;
      }

      if ( ! real_stop )
         return done();

      /*
      log("Start launch player release task...");
      // We're still holding resources, including the player itself.
      // Spin off a task to clean up, soon.
      //
      // No need to cancel this task.  The state is now STATE_STOP, all
      // events affecting the relevance of this thread move time on.
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
      log("Player release task started...");
      */

      log("Returning...");
      return done(State.STATE_STOP);
   }

   /* ********************************************************************
    * Pause/restart...
    */

   private int pause()
      { return pause("Paused."); }

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
                  stop();
               }
            }.start();

      player.pause();
      return done(State.STATE_PAUSE);
   }

   private int duck(String msg)
   {
      log("Duck: ", State.current());

      if ( State.is(State.STATE_DUCK) || ! State.is_playing() )
         return done();

      player.setVolume(0.1f, 0.1f);
      return done(State.STATE_DUCK);
   }

   private int restart()
   {
      log("Restart: ", State.current());

      if ( player == null || State.is_stopped() )
         return play();

      // Always reset the volume.
      // There's something broken about the state model.
      // For example, we could be in state DUCK, then buffering starts, so
      // suddenly we're in state BUFFERING, although we're also still ducked.
      // The probelm is that one state is being used to model two different
      // things.  Until that's fixed, it is nevertheless always safe (??)
      // reset the volume on restart.
      //
      player.setVolume(1.0f, 1.0f);

      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
         return done();

      if ( State.is(State.STATE_DUCK) )
         return done(State.STATE_PLAY);

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

      player.start();
      return done(State.STATE_PLAY);
   }

   private int click()
   {
      log("Click: ", State.current());

      if ( State.is(State.STATE_DISCONNECTED) )
      {
         stop();
         log("Click: cancel notification (disconnected)");
         Notify.cancel();
         return done();
      }

      if ( State.is_playing() && ! isNetworkUrl() )
         return pause();

      if ( State.is_playing() )
      {
         stop();
         log("Click: cancel notification (playing)");
         Notify.cancel();
         return done();
      }

      if ( player == null || State.is_stopped() )
         return play();

      if ( State.is(State.STATE_PAUSE) )
         return restart();

      log("Unhandled click: ", State.current());
      return done();
   }

   /* ********************************************************************
    * All onStartCommand() invocations end here...
    */

   private int done(String state)
   {
      if ( state != null )
         State.set_state(context, state, isNetworkUrl());

      return done();
   }

   private int done()
      { return START_NOT_STICKY; }

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
         State.set_state(context, State.STATE_PLAY, isNetworkUrl());

         // A launch is successful if there is no error within the first few
         // seconds.  If a launch is successful then later the stream fails,
         // then the launch will be repeated.  If it fails before it is
         // considered successful, then it will not be repeated.  This is
         // intented to prevent thrashing.
         //
         // No need to cancel this.  All events affecting the relevance of this
         // thread move time on.
         // TODO: Is that really true?
         // 
         new Later(10)
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
            State.set_state(context, State.STATE_BUFFER, isNetworkUrl());
            break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            State.set_state(context, State.STATE_PLAY, isNetworkUrl());
            break;
      }
      return true;
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra)
   {
      log("Error: ", ""+what);

      // Disabled in favour of logic in ./Connectivity.java
      //
      // if ( player != null
      //       && previous_launch_url != null
      //       && previous_launch_successful
      //       && URLUtil.isNetworkUrl(previous_launch_url)
      //       && Connectivity.isConnected(context) )
      // {
      //    player.reset();
      //    play_relaunch();
      //    return true;
      // }
      //
      // stop();
      State.set_state(context,State.STATE_ERROR, isNetworkUrl());
      new Later(300)
      {
         @Override
         public void later()
            { stop(); }
      }.start();

      /*
      switch ( what )
      {
         case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            stop();
            State.set_state(context,State.STATE_ERROR, isNetworkUrl());
            break;
         case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            stop();
            State.set_state(context,State.STATE_ERROR, isNetworkUrl());
            break;
         default:
            stop();
            State.set_state(context,State.STATE_ERROR, isNetworkUrl());
            break;
      }
      */

      // Returning true, here, prevents the onCompletionlistener from being called.
      //
      return true;
   }

   /* ********************************************************************
    * On completion listener...
    */

   @Override
   public void onCompletion(MediaPlayer mp)
   {
      log("Completion: " + State.current());

      // We only enter the completed state from a valid playing state.
      // This interacts with Connectivity and the error state.  When
      // connectivity is lost, we can get an error callback followed by a
      // completion callback.  In this case, we do not want to consider the
      // state to be complete.
      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_DUCK) )
         State.set_state(context, State.STATE_COMPLETE, isNetworkUrl());

      new Later(300)
      {
         @Override
         public void later()
            { stop(); }
      }.start();
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
               stop();
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
