package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.app.Notification;
import android.app.Notification.Builder;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.AudioManager;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

public class IntentPlayer extends Service {

   private static boolean debug = true;
   private static int nid = 1;

   private static MediaPlayer player = null;
   private static Context context = null;
   private static SharedPreferences prefs = null;

   private static String app_name = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   @Override
   public void onCreate() {
      context = getApplicationContext();
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
      app_name = getString(R.string.app_name);
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
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
      log(url);

      if ( intent_play.equals(action) )
         return play(url, startId);

      return Service.START_NOT_STICKY;
   }

   private int play(String url, int startId)
   {
      stop();

      if ( url.endsWith(".pls") )
         url = playlist(url);

      if ( url == null )
      {
         toast("No URL.");
         return Service.START_NOT_STICKY;
      }

      toast(url);
      player = new MediaPlayer();
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);

      player.setOnPreparedListener(
         new OnPreparedListener()
         {
            public void onPrepared(MediaPlayer player) {
               player.start();
               log("Ok.");
            }
         }
      );

      try
      {
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();
         startForeground(nid,
               new Notification.Builder(context)
                  .setContentTitle("Intent Radio")
                  .setContentText("Playing...")
                  .build() );
         log("Buffering...");
      }
      catch (Exception e)
      {
         toast("Error.");
         toast(e.getMessage());
         return stop();
      }

      return Service.START_NOT_STICKY;
   }

   private int stop()
   {
      if ( player != null )
      {
         toast("Stopping...");
         stopForeground(true);
         player.stop();
         player.reset();
         player.release();
         player = null;
      }
      else
         toast("Stopped.");
      return Service.START_NOT_STICKY;
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

}
