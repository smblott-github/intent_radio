package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;

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

import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

public class IntentPlayer extends Service {

   private static MediaPlayer player = null;
   private static Context context = null;

   private static String app_name = null;
   private static String intent_play = null;
   private static String intent_stop = null;

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {

      if ( app_name == null )
         app_name = getString(R.string.app_name);
      if ( intent_play == null )
         intent_play = getString(R.string.intent_play);
      if ( intent_stop == null )
         intent_stop = getString(R.string.intent_stop);
      if ( context == null )
         context = getApplicationContext();

      String action = intent.getStringExtra("action");
      if ( action == null )
         return Service.START_NOT_STICKY;

      String url = intent.hasExtra("url") ? intent.getStringExtra("url") : getString(R.string.default_url);
      log(action);
      log(url);

      if ( intent_play.equals(action) )
         return play(url);

      if ( intent_stop.equals(action) )
         return stop();

      return Service.START_STICKY;
   }

   private int play(String url)
   {
      stop();

      if ( url.endsWith(".pls") )
         url = playlist(url);

      if ( url == null )
      {
         toast("No URL.");
         return Service.START_NOT_STICKY;
      }

      player = new MediaPlayer();
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);

      try
      {
         toast(url);
         player.setDataSource(context, Uri.parse(url));
         player.setOnPreparedListener(
            new OnPreparedListener()
            {
               public void onPrepared(MediaPlayer player) {
                  player.start();
                  log("Ok.");
               }
            }
         );
         player.prepareAsync();
         log("Async.");
      }
      catch (Exception e)
      {
         toast("Error.");
         return stop();
      }

      return Service.START_STICKY;
   }

   private int stop()
   {
      if ( player != null )
      {
         toast("Stopping...");
         player.stop();
         player.reset();
         player.release();
         player = null;
      }
      else
         log("Stopped.");
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
            Log.d(app_name, text);
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
      Log.d(app_name, msg);
   }

   private void toast(String msg)
   {
      log(msg);
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

}
