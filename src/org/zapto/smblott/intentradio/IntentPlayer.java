package org.zapto.smblott.intentradio;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IntentPlayer extends Service {

   private MediaPlayer player = null;

   private static final String TAG = "IntentRadio";
   private static final String r4 = "http://www.bbc.co.uk/radio/listen/live/r4_heaacv2.pls";

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Context context = getApplicationContext();
      String action = intent.getStringExtra("action");
      if ( action == null )
         return Service.START_NOT_STICKY;
      String url = intent.hasExtra("url") ? intent.getStringExtra("url") : r4;

      if ( "org.zapto.smblott.intentradio.PLAY".equals(action) )
         play(context, url);

      if ( "org.zapto.smblott.intentradio.STOP".equals(action) )
         stop();

      return Service.START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

   void log(String msg)
   {
      Log.d(TAG, msg);
   }

   void toast(String msg)
   {
      log(msg);
      Context context = getApplicationContext();
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
   }

   void play(Context context, String url)
   {
      if ( url.endsWith(".pls") )
         url = playlist(url);

      if ( url == null )
      {
         toast("No URL.");
         return;
      }

      stop();

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
         stop();
      }
   }

   String playlist(String url)
   {
      log("Extract playlist:\n" + url);
      try {
         HttpClient client = new DefaultHttpClient();  
         HttpGet get = new HttpGet(url);
         HttpResponse response = client.execute(get);  
         HttpEntity entity = response.getEntity();  
         if (entity != null) {  
            String text = EntityUtils.toString(entity);
            Log.d(TAG, text);
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

   void stop()
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



}
