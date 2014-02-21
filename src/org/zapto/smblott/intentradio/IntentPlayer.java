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
import android.media.MediaPlayer.TrackInfo;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.lang.Runnable;
import java.lang.Thread;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

public class IntentPlayer extends Service
   implements OnBufferingUpdateListener, OnInfoListener, OnErrorListener, OnPreparedListener, Runnable
{

   private static final boolean debug = true;
   private static final int notification_id = 100;
   private static volatile boolean running = false;
   private static int counter = 0;
   private static IntentPlayer self = null;
   private static Thread thread = null;

   private static MediaPlayer player = null;
   private static Context context = null;
   private static SharedPreferences prefs = null;
   private static PendingIntent stop_intent = null;
   private static Builder builder = null;
   private static volatile Notification note = null;
   private static NotificationManager notification_manager = null;

   private static String app_name = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   @Override
   public void onCreate() {
      self = this;
      context = getApplicationContext();
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
      app_name = getString(R.string.app_name);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      notification_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      stop_intent = PendingIntent.getBroadcast(context, 0, new Intent(intent_stop), 0);

      builder =
         new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Intent Radio")
            .setContentText("Playing...")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setShowWhen(false)
            .setContentIntent(stop_intent)
            ;

   }

   public void onDestroy()
   {
      stop();
      ticker_stop();
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

      String url = intent.hasExtra("url") ? intent.getStringExtra("url") : getString(R.string.default_url);
      String name = intent.hasExtra("name") ? intent.getStringExtra("name") : url;

      if ( ! intent.hasExtra("url") && ! intent.hasExtra("name") )
         name = getString(R.string.default_name);

      log(url);
      log(name);

      if ( intent_play.equals(action) )
         return play(url, name, startId);

      return Service.START_NOT_STICKY;
   }

   private int play(String url, String name, int startId)
   {
      stop();

      builder.setContentTitle("Intent Radio");
      builder.setContentText(name);
      note = builder.build();

      if ( url != null && url.endsWith(".pls") )
         url = playlist(url);

      if ( url == null )
      {
         toast("Intent Radio: URL not found.");
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
         // What if URL fails to parse?
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
         startForeground(notification_id, note);
         log("Buffering...");
      }
      catch (Exception e)
      {
         toast("Error.");
         toast(e.getMessage());
         return stop();
      }

      // ticker();
      return Service.START_NOT_STICKY;
   }

   private int stop()
   {
      ticker_stop();
      if ( player != null )
      {
         toast("Stopping...");
         stopForeground(true);
         player.stop();
         player.reset();
         player.release();
         player = null;
      }
      // else
      //    toast("Stopped.");
      note = null;
      return Service.START_NOT_STICKY;
   }

   public void onBufferingUpdate(MediaPlayer player, int percent)
   {
      if ( 0 <= percent && percent <= 100 )
         log("Buffering: " + percent + "%"); 
      // else
      //    log("Buffering: garbage value"); 
   }

   public boolean onInfo(MediaPlayer player, int what, int extra)
   {
      log("Info!");
      return true;
   }

   public boolean onError(MediaPlayer player, int what, int extra)
   {
      log("Error!");
      return true;
   }

   public void onPrepared(MediaPlayer player) {
      player.start();
      log("Ok (onPrepared).");
   }

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
            log(text);

            ArrayList urls = links(text);
            if ( 0 < urls.size() )
               return (String) urls.get(0);
            else
               toast("Could not extract URLs from playlist.");
         }
      } catch (Exception e) {
         toast("Error fetching playlist.");
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

   private void log(String msg)
   {
      if ( debug && msg != null )
         Log.d(app_name, msg);
   }

   private void toast(String msg)
   {
      if ( msg != null )
      {
         log(msg);
         Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
      }
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

   /*
   private void mk_looper()
   {
      thread = new Thread() {
         public void run()
         {
            Looper.prepare();
            looper = Looper.myLooper();
            handler = new Handler(looper,self);
            Looper.loop();
         }
      };
      thread.start();
   }

   public boolean handleMessage(Message msg)
   {
      log("handleMessage");
      return true;
   }

   private void mk_looper2()
   {
      handlerthread = new HandlerThread("Looper");
      looper = handlerthread.getLooper();
      handlerthread.start();

      handler = new Handler(looper)
      {
         @Override
         public void handleMessage(Message message)
         {
            log("handleMessage");
         }
      };

   }
   */

   private String make_time(int position)
   {
      int seconds = position / 1000;
      int minutes = seconds / 60; seconds = seconds % 60;
      int hours = minutes / 60; minutes = minutes % 60;

      String sSeconds = "" + seconds;
      String sMinutes = "" + minutes;

      if ( seconds < 10 )              sSeconds = "0" + sSeconds;
      if ( minutes < 10 && 0 < hours ) sMinutes = "0" + sMinutes;

      String time = sMinutes + ":" + sSeconds;

      if ( 0 < hours )
         time = "" + hours + ":" + time;

      // log(time);
      return time;
   }

   private void tick()
   {
      // counter += 1;
      // log("tick " + counter);
      if ( player != null )
      {
         int position = player.getCurrentPosition();
         // log("position " + position);
         if ( note != null )
         {
            // Concurrency?
            builder.setContentText(make_time(position));
            note = builder.build();
            notification_manager.notify( notification_id, note);
         }
      }
   }

   public void run()
   {
      while ( running )
      {
         try {
            Thread.sleep(1000);
            tick();
         }
         catch (Exception e) {
            log("Thread.sleep() error; interrupted?.");
            running = false;
         }
      }
      log("Thread.run() done.");
   }

   private void ticker()
   {
      if ( thread == null )
      {
         running = true;
         thread = new Thread(this);
         thread.start();
      }
   }

   private void ticker_stop()
   {
      if ( thread != null )
      {
         log("ticker_stop");
         thread.interrupt();
         // try { thread.join(); }
         // catch (Exception e) { log("Thread.join() error"); }
      }
      running = false;
      thread = null;
   }

}
