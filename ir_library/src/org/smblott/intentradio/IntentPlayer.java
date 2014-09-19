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

      if ( ! URLUtil.isValidUrl(url) )
      {
         toast("Invalid URL.");
         return stop();
      }

      if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) )
      {
         toast("No internet connection.");
         // We'll pretend that we dropped the connection.  That way, when we
         // get a connection, playback will start.
         connectivity.dropped_connection();
         return done();
      }

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         toast("Could not obtain audio focus.");
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

      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, app_name_long);

      log("Connecting...");
      playlist_task = new Playlist(this,url).start();

      // The Playlist object calls play_launch(url), when it's ready.
      return done(State.STATE_BUFFER);
   }

   /* ********************************************************************
    * Launch player...
    */

   // The launch_url may be different from the original URL.  For example, it
   // could be the URL extracted from a playlist, whereas the original url is
   // that of the playlist itself.
   private static String launch_url = null;

   public int play_launch(String url)
   {
      log("Launching: ", url);

      launch_url = null;
      if ( ! URLUtil.isValidUrl(url) )
      {
         toast("Invalid URL.");
         return stop();
      }

      launch_url = url;

      // Note:  Because of the way we handle network connectivity, the player
      // always stops and then restarts as we move between network types.
      // Therefore, stop() and start() are always called.  So we always have
      // the WiFi lock if we're on WiFi and we need it, and don't otherwise.
      // 
      // Here, we could be holding a WiFi lock because the playlist URL was a
      // network URL, but perhaps now the launch URL is not.  Or the other way
      // around.  So release the WiFi lock (if it's being held) and reaquire
      // it, if necessary.
      WifiLocker.unlock();
      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, app_name_long);

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

   @Override
   public void onPrepared(MediaPlayer mp)
   {
      if ( mp == player )
      {
         log("Starting....");
         player.start();
         State.set_state(context, State.STATE_PLAY, isNetworkUrl());
      }
   }

   public boolean isNetworkUrl()
      { return isNetworkUrl(launch_url); }

   public boolean isNetworkUrl(String check_url)
      { return ( check_url != null && URLUtil.isNetworkUrl(check_url) ); }

   /* ********************************************************************
    * Stop...
    */

   public int stop()
      { return stop(true); }

   private int stop(boolean update_state)
   {
      log("Stopping");

      Counter.time_passes();
      launch_url = null;
      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      if ( player != null )
      {
         log("Stopping/releasing player...");
         if ( player.isPlaying() )
            player.stop();
         player.reset();
         player.release();
         player = null;
      }

      if ( playlist_task != null )
      {
         playlist_task.cancel(true);
         playlist_task = null;
      }

      if ( update_state )
         return done(State.STATE_STOP);
      else
         return done();
   }

   /* ********************************************************************
    * Reduce volume, for a short while, for a notification.
    */

   private int duck(String msg)
   {
      log("Duck: ", State.current());

      if ( State.is(State.STATE_DUCK) || ! State.is_playing() )
         return done();

      player.setVolume(0.1f, 0.1f);
      return done(State.STATE_DUCK);
   }

   /* ********************************************************************
    * Pause/restart...
    */

   private int pause()
   {
      log("Pause: ", State.current());

      if ( player == null || State.is(State.STATE_PAUSE) || ! State.is_playing() )
         return done();

      if ( pause_task != null )
         pause_task.cancel(true);

      // We're still holding resources, including a possibly a Wifi Wakelock
      // and the player itself.  Spin off a task to convert this "pause"
      // into a stop, soon.
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
         toast("Failed to acquire audio focus.");
         return done();
      }

      if ( pause_task != null )
         { pause_task.cancel(true); pause_task = null; }

      player.start();
      return done(State.STATE_PLAY);
   }

   /* ********************************************************************
    * Respond to click events from the notification.
    */

   private int click()
   {
      log("Click: ", State.current());

      if ( State.is(State.STATE_DISCONNECTED) )
      {
         stop();
         Notify.cancel();
         return done();
      }

      if ( State.is_playing() && ! isNetworkUrl() )
         return pause();

      if ( State.is_playing() )
      {
         stop();
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
   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      /*
      // Notifications of buffer state seem to be unreliable.
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

   private void stop_soon()
   {
      new Later(300)
      {
         @Override
         public void later()
            { stop(); }
      }.start();
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra)
   {
      log("Error: ", ""+what);

      State.set_state(context,State.STATE_ERROR, isNetworkUrl());
      stop_soon();

      // Returning true, here, prevents the onCompletionlistener from being called.
      return true;
   }

   /* ********************************************************************
    * On completion listener...
    */

   @Override
   public void onCompletion(MediaPlayer mp)
   {
      log("Completion: " + State.current());

      // We only enter STATE_COMPLETE for non-network URLs, and only if we
      // really were playing (so not, for example, if we are in STATE_ERROR, or
      // STATE_DISCONNECTED).  This simplifies connectivity management, in
      // Connectivity.java.
      log("onCompletion: isNetworkUrl: " + isNetworkUrl());
      if ( ! isNetworkUrl() && (State.is(State.STATE_PLAY) || State.is(State.STATE_DUCK)) )
         State.set_state(context, State.STATE_COMPLETE, isNetworkUrl());

      // Don't stay completed for long. stop(), soon, to free up resources.
      stop_soon();
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
               log("Audiofocus_gain");
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               log("Transient");
               // pause();
               // break;
               // Drop through.

            case AudioManager.AUDIOFOCUS_LOSS:
               log("Audiofocus_loss");
               pause();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
               log("Audiofocus_loss_transient_can_duck");
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
