package org.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

   public  static String name = "";
   public  static String url = "";

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
      stop();

      if ( player != null )
      {
         player.release();
         player = null;
      }

      log("Destroyed.");
      Logger.state("off");
      super.onDestroy();
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
         /* Ensure intent has a "url"...
          */
         if ( ! intent.hasExtra("url") || intent.getStringExtra("url").equals("") )
         {
            if ( url == null || url.equals("") )
            {
               intent.putExtra("url", default_url);
               intent.putExtra("name", default_name);
            }
            else
            {
               intent.putExtra("url", url);
               intent.putExtra("name", name);
            }
         }

         /* Ensure intent has a "name"...
          */
         if ( ! intent.hasExtra("name") )
            intent.putExtra("name", intent.getStringExtra("url"));

         /* Extract "url" and "name"...
          */
         url = intent.getStringExtra("url");
         name = intent.getStringExtra("name");

         Editor editor = settings.edit();
         editor.putString("url", url);
         editor.putString("name", name);
         editor.commit();

         log("Name: ", name);
         log("URL: ", url);
         return play(url);
      }

      if ( action.equals(intent_state_request) )
      {
         State.get_state(context);
         return done();
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
      stop(false);
      toast(name);

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return stop("Failed to get audio focus!");

      builder
         .setOngoing(true)
         .setContentIntent(pending_stop)
         .setContentText("Connecting...");
      note = builder.build();

      WifiLocker.lock(context, app_name_long);
      startForeground(note_id, note);
      log("Connecting...");

      // /////////////////////////////////////////////////////////////////
      // Set up media player...

      log("Play: ", url);

      if ( player == null )
      {
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

      on_first_launch();

      // /////////////////////////////////////////////////////////////////
      // Playlists...

      if ( url.endsWith(PlaylistPls.suffix) )
         pltask = new PlaylistPls(this);

      if ( url.endsWith(PlaylistM3u.suffix) )
         pltask = new PlaylistM3u(this);

      if ( pltask != null )
      {
         log("Playlist: ", url);
         notificate("Fetching playlist...");
         pltask.execute(url);
         return done(State.STATE_BUFFER);
      }

      // /////////////////////////////////////////////////////////////////
      // Ready...

      return play_launch(url);
   }

   /* ********************************************************************
    * Launch player...
    */

   private static String last_launch_url = null;

   private void play_relaunch(int then)
      { log("Relaunch."); play_launch(last_launch_url, then); }

   public int play_launch(String url, int then)
   {
      if ( url != null && Counter.still(then) )
         return play_launch(url);
      return stop();
   }

   public int play_launch(String url)
   {
      last_launch_url = url;
      log("Launching: ", url);
      notificate("Connecting...");

      try
      {
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

   private int stop(boolean send_state)
      { return stop(true,null,send_state); }

   private int stop()
      { return stop(true,null,true); }

   private int stop(String msg)
      { return stop(false,msg,true); }

   private int stop(boolean kill_note, String text, boolean send_state)
   {
      log("Stopping kill_note: ", ""+kill_note);
      log("Stopping send_state: ", ""+send_state);
      if ( text != null )
         log("Stopping: ", text);

      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      // Time moves on...
      //
      Counter.time_passes();
      last_launch_url = null;

      // Cancel any outstanding asynchronous playlist task...
      // 
      // Note: We may in fact be calling this from within the onPostExecute
      // method of the task itself (because onPostExecute calls "play", which
      // calls "stop").
      //
      if ( pltask != null && ! pltask.finished() )
         pltask.cancel(true);
      pltask = null;

      // Stop player...
      //
      if ( player != null )
      {
         log("Stopping player...");
         if ( player.isPlaying() )
            player.stop();
         player.reset();
         // player.release();
         // player = null;
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
      }

      return done(send_state ? State.STATE_STOP : null);
   }

   /* ********************************************************************
    * Pause/restart...
    */

   private int pause()
      { return pause("Paused..."); }

   private int pause(String msg)
   {
      if ( player == null )
         return done(State.STATE_STOP);

      if ( ! player.isPlaying() )
         return done();

      new Later() {
         @Override
         public void finish()
            { stop(false,"Suspended. Click to restart.",true); }
      }.execute();

      notificate(msg);
      player.pause();
      return done(State.STATE_PAUSE);
   }

   private int restart()
   {
      if ( player == null || State.is(State.STATE_STOP) || State.is(State.STATE_ERROR)  )
      {
         try
            { pending_play.send(); }
         catch (Exception e)
            { log("Failed to deliver pending_play from restart()."); }
         return done();
      }

      if ( player.isPlaying() )
         return done();

      if ( ! State.is(State.STATE_PAUSE) )
         return done();

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
      {
         toast("Intent Radio:\nFailed to (re-)acquire audio focus.");
         return done();
      }

      // Time must pass. Because if we were paused, then
      // there will be a thread out there waiting to stop() us.
      //
      Counter.time_passes();
      // Because time passes, we better also move forward the relaunch() time
      // stamp.
      //
      on_first_launch();
      //
      startForeground(note_id, note);
      notificate();
      player.start();
      return done(State.STATE_PLAY);
   }

   /* ********************************************************************
    * All onStartCommand invocations end here...
    */

   private int done()
      { return done(null); }

   private int done(String state)
   {
      State.set_state(context, state);
      return START_NOT_STICKY;
   }

   /* ********************************************************************
    * Listeners...
    */

   @Override
   public void onPrepared(MediaPlayer a_player)
   {
      if ( a_player == player )
      {
         log("Prepared, starting....");
         player.start();
         notificate();
         State.set_state(context, State.STATE_PLAY);
      }
   }

   @Override
   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: ", ""+percent, "%"); 
   }

   @Override
   public boolean onInfo(MediaPlayer player, int what, int extra)
   {
      switch (what)
      {
         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            log("Buffering/end");
            notificate();
            State.set_state(context, State.STATE_PLAY);
            break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            log("Buffering/start");
            notificate("Buffering...");
            State.set_state(context, State.STATE_BUFFER);
            break;

         case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            log("MEDIA_INFO_BAD_INTERLEAVING");
            State.set_state(context, State.STATE_ERROR);
            break;
         case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            log("MEDIA_INFO_NOT_SEEKABLE");
            // State.set_state(context, State.STATE_ERROR);
            break;
         case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            log("MEDIA_INFO_METADATA_UPDATE");
            // State.set_state(context, State.STATE_ERROR);
            break;
      }
      return true;
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra)
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
            stop("Unknown media-player error.");
            break;
      }
      return true;
   }

   /* ********************************************************************
    * On completion listener...
    *
    * This should only be called (and matter, because the time counter hasn't
    * progressed) if there is an error with the stream.  So we'll try
    * restarting it, but only a limited number of times.
    */

   private static final int restart_max = 3;
   private static       int restart_cnt = 0;
   private static       int restart_now = 0;

   private void on_first_launch()
   {
      restart_cnt = restart_max;
      restart_now = Counter.now();
      log("On first launch: ", "now="+restart_now);
   }

   @Override
   public void onCompletion(MediaPlayer a_player)
   {
      if ( 0 < restart_cnt )
      {
         log("Completion, attempt restart: ", "now="+restart_now, " cnt="+restart_cnt);
         restart_cnt -= 1;
         notificate("Re-connecting...");
         play_relaunch(restart_now);
      }
      else
         log("Completion, not restarting: ", "now="+restart_now, " cnt="+restart_cnt);
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
               log("Audio focus: AUDIOFOCUS_GAIN");
               player.setVolume(1.0f, 1.0f);
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS:
               log("Audio focus: AUDIOFOCUS_LOSS");
               pause("Audio focus lost, paused...");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               log("Audio focus: AUDIOFOCUS_LOSS_TRANSIENT");
               pause("Audio focus lost, paused...");
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
               log("Audio focus: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
               player.setVolume(0.1f, 0.1f);
               notificate("Focus lost, quiet mode...");
               State.set_state(context, State.STATE_DIM);
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
               .setContentIntent(ongoing ? pending_stop : pending_restart)
               .setOngoing(ongoing)
               .setContentText(msg == null ? name : msg)
               .build();
         log("Notificate: ", msg == null ? name : msg);
         log("Notificate click: ", ""+(ongoing ? "stop" : "restart") );
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

   @Override
   public IBinder onBind(Intent intent)
      { return null; }
}
