package org.smblott.intentradio;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.TextUtils;
import java.util.List;
import android.os.AsyncTask;
import android.webkit.URLUtil;

public abstract class Playlist extends AsyncTask<String, Void, String>
{
   private static final int max_ttl = 10;

   private IntentPlayer player = null;
   private int then = 0;

   Playlist(IntentPlayer a_player)
   {
      super();
      player = a_player;
      then = Counter.now();
      log("Playlist: then=" + then);
   }

   abstract String filter(String line);

   protected String doInBackground(String... args)
   {
      String url = args[0];
      int ttl = max_ttl;

      while ( 0 < ttl && url != null && is_playlist(url) )
      {
         log("Playlist url: ", url);

         if ( ! URLUtil.isValidUrl(url) )
         {
            log("Invalid URL.");
            url = null;
            break;
         }

         url = fetch_url(url);
         ttl -= 1;
      }

      if ( ttl == 0 && is_playlist(url) )
      {
         log("Playlist: too many playlists.");
         url = null;
      }

      if ( url == null )
         log("Playlist: failed to extract URL from playlist.");
      else
         log("Playlist final url: ", url);

      return url;
   }

   public static boolean is_playlist(String url)
      { return PlaylistPls.is_playlist(url) || PlaylistM3u.is_playlist(url); }

   // This runs on the main thread...
   //
   protected void onPostExecute(String url) {
      if ( url != null && player != null && ! isCancelled() && Counter.still(then) )
         player.play_launch(url);
   }

   /* ********************************************************************
    * Fetch a single (random) url from a playlist...
    */

   private static Random random = null;

   private String fetch_url(String url)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
         log("Playlist lines: ", lines.get(i));

      for (int i=0; i<lines.size(); i+= 1)
         lines.set(i, filter(lines.get(i).trim()));

      for (int i=0; i<lines.size(); i+= 1)
         if ( lines.get(i).length() != 0 )
            log("Playlist filtered: ", lines.get(i));

      List<String> links = get_links(TextUtils.join("\n", lines));
      if ( links.size() == 0 )
         return null;

      for (int i=0; i<links.size(); i+= 1)
         log("Playlist links: ", links.get(i));

      if ( random == null )
         random = new Random();

      return links.get(random.nextInt(links.size()));
   }

   /* ********************************************************************
    * Extract list of urls from string...
    *
    * source: http://blog.houen.net/java-get-url-from-string/
    */

   private static final String url_regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
   private static Pattern url_pattern = null;

   private static List<String> get_links(String text)
   {
      ArrayList links = new ArrayList<String>();

      if ( url_pattern == null )
         url_pattern = Pattern.compile(url_regex);

      Matcher matcher = url_pattern.matcher(text);

      while( matcher.find() )
      {
         String str = matcher.group();
         if (str.startsWith("(") && str.endsWith(")"))
            str = str.substring(1, str.length() - 1);
         links.add(str);
      }

      return links;
   }

   /* ********************************************************************
    * MIME types...
    */

   public static boolean is_playlist_mime_type(String mime)
   {
      if ( mime == null )
         return false;

      // Is this (likely to be) a valid MIME type?
      //
      // We can be quite loose, here.  We're really just trying to avoid downloading
      // a media URL as a playlist.
      //
      if ( mime.equals("audio/x-scpls")                 ) return true;
      if ( mime.equals("audio/scpls")                   ) return true;
      if ( mime.equals("audio/x-mpegurl")               ) return true;
      if ( mime.equals("audio/mpegurl")                 ) return true;
      if ( mime.equals("audio/mpeg-url")                ) return true;
      if ( mime.equals("application/vnd.apple.mpegurl") ) return true;
      if ( mime.equals("application/x-winamp-playlist") ) return true;
      if ( mime.equals("application/music")             ) return true;
      if ( mime.indexOf("text/") == 0                   ) return true;

      // Catch alls...
      //
      if ( mime.indexOf("mpegurl")  != -1 ) return true;
      if ( mime.indexOf("mpeg-url") != -1 ) return true;
      if ( mime.indexOf("scpls")    != -1 ) return true;

      Logger.log("Playlist - not a valid MIME type: ", mime);
      return false;
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }
}
