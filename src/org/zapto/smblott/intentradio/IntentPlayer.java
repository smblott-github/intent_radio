package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;

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

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

   private static final boolean debug = false;
   private static final int note_id = 100;

   private static MediaPlayer player = null;
   private static Context context = null;
   private static PendingIntent pend_intent = null;
   private static Builder builder = null;
   private static volatile Notification note = null;
   private static NotificationManager notification_manager = null;

   private static String app_name = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   private static FileOutputStream log_file_stream = null;
   private static DateFormat format = null;

   private static String name = null;
   private static String url = null;

   /* ********************************************************************
    * Service methods...
    */

   @Override
   public void onCreate() {
      context = getApplicationContext();
      app_name = getString(R.string.app_name);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      notification_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      format = new SimpleDateFormat("HH:mm:ss ");
      pend_intent = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);

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
            .setShowWhen(false)
            .setContentIntent(pend_intent)
            ;
   }

   public void onDestroy()
   {
      if ( log_file_stream != null )
      {
         try { log_file_stream.close(); }
         catch (Exception e) { }
         log_file_stream = null;
      }
      stop();
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      if ( intent == null )
         return Service.START_NOT_STICKY;

      String action = intent.getStringExtra("action");
      if ( action == null )
         return Service.START_NOT_STICKY;
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

      return Service.START_NOT_STICKY;
   }

   /* ********************************************************************
    * Play...
    */

   private int play(String url)
   {
      stop();

      builder.setContentTitle("Intent Radio");
      builder.setContentText("Connecting...: " + name);
      note = builder.build();

      if ( url != null )
         if ( url.endsWith(".pls") )
            url = playlist(url);

      if ( url == null )
      {
         toast("No URL.", true);
         return Service.START_NOT_STICKY;
      }

      toast(name);
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
      notificate("Playing");
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
         case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            msg += "/media unsupported"; break;
         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            msg += "/buffering start.."; break;
         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            msg += "/buffering end"; break;
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
      return true;
   }

   /* ********************************************************************
    * Extract URL from a playlist...
    */

   private String playlist(String url)
   {
      log("Extract playlist:\n" + url);
      try {
         HttpClient client = new DefaultHttpClient();  
         HttpGet get = new HttpGet(url);
         HttpResponse response = client.execute(get);  
         HttpEntity entity = response.getEntity();  
         if (entity != null) {  
            String text = EntityUtils.toString(entity);

            ArrayList urls = links(text);
            if ( 0 < urls.size() )
               return (String) urls.get(0);
            else
               toast("Could not extract URLs from playlist.", true);
         }
      } catch (Exception e) {
         toast("Error fetching playlist.", true);
      }
      return null;
   }

   // http://blog.houen.net/java-get-url-from-string/
   private ArrayList links(String text)
   {
      ArrayList links = new ArrayList();
    
      String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(text);

      while( m.find() )
      {
         String urlStr = m.group();
         if (urlStr.startsWith("(") && urlStr.endsWith(")"))
            urlStr = urlStr.substring(1, urlStr.length() - 1);
         links.add(urlStr);
      }

      return links;
   }

   /* ********************************************************************
    * Logging...
    */

   private void log_to_file(String msg)
   {
      if ( log_file_stream == null || msg == null )
         return;

      try
      {
         String stamp = format.format(new Date());
         log_file_stream.write((stamp+msg+"\n").getBytes());
         log_file_stream.flush();
      } catch (Exception e) {}
   }

   private void log(String msg)
   {
      if ( msg == null )
         return;

      log_to_file(msg);
      if ( debug )
         Log.d(app_name, msg);
   }

   private void toast(String msg, boolean log_too)
   {
      if ( msg == null )
         return;

      Toast.makeText(context, "Intent Radio: \n" + msg, Toast.LENGTH_SHORT).show();
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
         builder.setContentText(msg == null ? name : (msg + ": " + name));
         note = builder.build();
         notification_manager.notify( note_id, note);
      }
   }

   /* ********************************************************************
    * Required abstract method...
    */

   public IBinder onBind(Intent intent) {
      return null;
   }

}
