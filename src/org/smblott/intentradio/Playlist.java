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

public abstract class Playlist extends AsyncTask<String, Void, Void>
{
   private static final String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";

   private Context context = null;
   private String intent_play = null;

   Playlist(Context ctx, String play_intent)
   {
      super();
      context = ctx;
      intent_play = play_intent;
   }

   // A playlist contains a sequence of lines.  Only some of those lines may
   // contain URLs.  Other lines such as comments may also contain URLs, but
   // they aren't part of the playlist.
   //
   // Implementations of keep() must return true if a line of a playlist files
   // may contain a URL, false, otherwise.
   //
   // An implementation which always returns true may work, but may also
   // occasionally stumble across a URL which is not part of the playlist.
   //
   abstract boolean keep(String line);

   /* ********************************************************************
    * Asynchronous task to fetch playlist...
    */

   protected Void doInBackground(String... args)
   {
      if ( args.length != 3 )
      {
         log("PlaylistPlsGetter: invalid args length");
         return null;
      }

      String url = args[0];
      String name = args[1];
      int cnt = Integer.parseInt(args[2]);

      if ( url == null )
         log("PlaylistPlsGetter: no playlist url");

      if ( name == null )
         log("PlaylistPlsGetter: no name");

      if ( url == null || name == null )
         return null;

      url = get(url);

      if ( url == null )
      {
         log("PlaylistPlsGetter: failed to extract url");
         return null;
      }

      if ( url.endsWith(PlaylistPls.suffix) )
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

   /* ********************************************************************
    * Fetch a single (random) url from a playlist...
    */

   public String get(String url)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
         if ( ! keep(lines.get(i)) )
            lines.set(i, "");

      List<String> links = getLinks(TextUtils.join("\n", lines));
      if ( links.size() == 0 )
         return null;

      return links.get(new Random().nextInt(links.size()));
   }

   /* ********************************************************************
    * Extract list of urls from string...
    *
    * source: http://blog.houen.net/java-get-url-from-string/
    */

   private List<String> getLinks(String text)
   {
      ArrayList links = new ArrayList<String>();
    
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(text);

      while( m.find() )
      {
         String str = m.group();
         if (str.startsWith("(") && str.endsWith(")"))
            str = str.substring(1, str.length() - 1);
         links.add(str);
      }

      return links;
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String msg)
      { Logger.log(msg); }
}
