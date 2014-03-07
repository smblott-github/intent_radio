package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.TextUtils;
import java.util.List;
import android.os.AsyncTask;
import android.util.Log;

public abstract class Playlist extends AsyncTask<String, Void, Void>
{
   Context context = null;
   String intent_play = null;

   Playlist(Context ctx, String play_intent)
   {
      super();
      context = ctx;
      intent_play = play_intent;
   }

   abstract boolean keep(String line);

   public String get(String url)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
         if ( ! keep(lines.get(i)) )
            lines.set(i, "");

      ArrayList links = links(TextUtils.join("\n", lines));
      if ( links.size() == 0 )
         return null;

      return (String) links.get(new Random().nextInt(links.size()));
   }

   // source: http://blog.houen.net/java-get-url-from-string/
   //
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

   protected Void doInBackground(String... args)
   {
      if ( args.length != 3 )
      {
         log("PlaylistPlsGetter: invalid args length");
         return null;
      }

      String plsUrl = args[0];
      String name = args[1];
      int cnt = Integer.parseInt(args[2]);

      if ( plsUrl == null )
      {
         log("PlaylistPlsGetter: no playlist url");
         return null;
      }

      if ( name == null )
      {
         log("PlaylistPlsGetter: no name");
         return null;
      }

      String url = get(plsUrl);

      if ( url == null )
      {
         log("PlaylistPlsGetter: failed to extract url");
         return null;
      }

      if ( keep(url) )
      {
         log("Playlist: another paylist!");
         return null;
      }

      Intent msg = new Intent(context, IntentPlayer.class);
      msg.putExtra("action", intent_play);
      msg.putExtra("url", url);
      msg.putExtra("name", name);
      msg.putExtra("cnt", cnt);

      if ( ! isCancelled() )
         context.startService(msg);

      return null;
   }

   private static void log(String msg)
   {
      if ( msg != null )
         Log.d("IntentRadio", msg);
   }
}
