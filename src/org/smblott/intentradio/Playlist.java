package org.smblott.intentradio;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import android.os.AsyncTask;
import android.webkit.URLUtil;
import android.net.Uri;
// import java.util.concurrent.ThreadPoolExecutor;

public class Playlist extends AsyncTask<Void, Void, String>
{
   private static final int max_ttl = 10;

   // Enumeration for playlist types.
   //
   private static final int NONE    = 0;
   private static final int M3U     = 1;
   private static final int PLS     = 2;

   private IntentPlayer player = null;
   private String start_url = null;
   private int then = 0;

   Playlist(IntentPlayer a_player, String a_url)
   {
      super();
      player = a_player;
      start_url = a_url;
      then = Counter.now();
      log("Playlist: then=" + then);
   }

   public void start()
   {
      // log("ThreadPoolExecutor cores: ", ""+((ThreadPoolExecutor)AsyncTask.THREAD_POOL_EXECUTOR).getCorePoolSize());
      // log("ThreadPoolExecutor max: ", ""+((ThreadPoolExecutor)AsyncTask.THREAD_POOL_EXECUTOR).getMaximumPoolSize());
      //
      // Start this on a THREAD_POOL_EXECUTOR.  If it runs as a regular
      // AsyncTask, then it can interfere with loading of the app's UI.  So,
      // use regular AsyncTasks for the UI, and THREAD_POOL_EXECUTOR tasks for
      // the service.
      //
      executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
   }

   protected String doInBackground(Void... args)
   {
      String url = start_url;
      int ttl = max_ttl;
      int type = NONE;

      if ( url != null && url.length() != 0 && URLUtil.isValidUrl(url) )
         type = playlist_type(url);
      else
         url = null;

      while ( 0 < ttl && url != null && type != NONE )
      {
         ttl -= 1;
         log("Playlist url: ", url);
         log("Playlist type: ", ""+type);

         url = select_url_from_playlist(url,type);
         if ( url != null && url.length() != 0 && URLUtil.isValidUrl(url) )
            type = playlist_type(url);
         else
            url = null;
      }

      if ( url  == null ) { log("Playlist: failed to extract url."     );             }
      if ( ttl  == 0    ) { log("Playlist: too many playlists (TTL)."  ); url = null; }
      if ( type != NONE ) { log("Playlist: too many playlists (TYPE)." ); url = null; }
      if ( url  != null ) { log("Playlist final url: ", url            );             }

      return url;
   }

   // This runs on the main thread...
   //
   protected void onPostExecute(String url) {
      if ( url != null && player != null && ! isCancelled() && Counter.still(then) )
         player.play_launch(url);
      else
         log("Playlist: launch cancelled");
   }

   /* ********************************************************************
    * Filter lines of a playlist...
    */

   static String filter(String line, int type)
   {
      switch (type)
      {
         //
         case M3U:
            return line.indexOf('#') == 0 ? "" : line;
         //
         case PLS:
            if ( line.startsWith("File") && 0 < line.indexOf('=') )
               return line;
            return "";
         //
         default:
         // Should not happen.
            log("Playlist invalid filter type: ", line);
            return line;
      }
   }

   /* ********************************************************************
    * Select a single (random) url from a playlist...
    */

   private static Random random = null;

   private String select_url_from_playlist(String url, int type)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
      {
         String line = lines.get(i);
         log("Playlist lines: ", line);
         line = filter(line.trim(),type);
         if ( 0 < line.length() )
            log("Playlist filtered: ", line);
         lines.set(i, line);
      }

      List<String> links = select_urls_from_list(lines);
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

   private static List<String> select_urls_from_list(List<String> lines)
   {
      ArrayList links = new ArrayList<String>();

      if ( url_pattern == null )
         url_pattern = Pattern.compile(url_regex);

      for (int i=0; i<lines.size(); i+=1)
      {
         String line = lines.get(i);
         if ( 0 < line.length() )
         {
            Matcher matcher = url_pattern.matcher(line);
            if ( matcher.find() )
            {
               String link = matcher.group();
               if (link.startsWith("(") && link.endsWith(")"))
                  link = link.substring(1, link.length() - 1);
               links.add(link);
            }
         }
      }

      return links;
   }

   /* ********************************************************************
    * Suffix utilities...
    */

   private static Uri parse_uri(String url)
      { return Uri.parse(url); }

   private static boolean is_suffix(String text, String suffix)
      { return text != null && text.endsWith(suffix) ; }

   private static boolean is_some_suffix(String url, String suffix)
   {
      if ( is_suffix(url, suffix) )
         return true;

      return is_suffix(parse_uri(url).getPath(), suffix);
   }

   // We rely only on the suffix, here.
   // It seems that checking the actual MIME Content-Type
   // returned by the server is not reliable, in practice.
   //
   private static int playlist_type(String url)
   {
      url = url.toLowerCase();
      if ( is_some_suffix(url,".m3u"  ) ) return M3U;
      if ( is_some_suffix(url,".m3u8" ) ) return M3U;
      if ( is_some_suffix(url,".pls"  ) ) return PLS;
      return NONE;
   }

   /* ********************************************************************
    * MIME types...
    */

   // Is this (likely to be) a playlist MIME type?
   //
   // We can be quite loose here.  We're just avoiding downloading
   // a media URL as a playlist.
   //
   public static boolean is_playlist_mime_type(String mime)
   {
      if ( mime == null )
         return false;

      if ( mime.equals("audio/x-scpls")                 ) return true;
      if ( mime.equals("audio/scpls")                   ) return true;
      if ( mime.equals("audio/x-mpegurl")               ) return true;
      if ( mime.equals("audio/mpegurl")                 ) return true;
      if ( mime.equals("audio/mpeg-url")                ) return true;
      if ( mime.equals("application/vnd.apple.mpegurl") ) return true;
      if ( mime.equals("application/x-winamp-playlist") ) return true;

      // Catch alls...
      //
      if ( mime.indexOf("mpegurl")  != -1 ) return true;
      if ( mime.indexOf("mpeg-url") != -1 ) return true;
      if ( mime.indexOf("scpls")    != -1 ) return true;
      if ( mime.indexOf("text/")    ==  0 ) return true;

      Logger.log("Playlist - not a valid MIME type: ", mime);
      return false;
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }
}
