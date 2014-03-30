package org.smblott.intentradio;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

import java.util.List;
import java.util.ArrayList;
import android.text.TextUtils;

public class HttpGetter
{
   public static List<String> httpGet(String str)
   {
      HttpURLConnection connection = null;
      List<String> lines = new ArrayList<String>();

      try
      {
         URL url = new URL(str);
         connection = (HttpURLConnection) url.openConnection();

         String mime = connection.getContentType();
         if ( mime == null )
            return lines;

         Logger.log("HttpGetter mime type: ", mime);
         if ( ! Playlist.is_playlist_mime_type(mime) )
         {
            Logger.log("HttpGetter: invalid mime type");
            return lines;
         }

         InputStream stream = new BufferedInputStream(connection.getInputStream());
         readStream(stream, lines);
      }
      catch ( Exception e )
         { if ( connection != null ) connection.disconnect(); }

      return lines;
   }

   private static void readStream(InputStream stream, List<String> lines) throws Exception
   {
      String line;
      BufferedReader buff = new BufferedReader(new InputStreamReader(stream));

      while ((line = buff.readLine()) != null)
         lines.add(line);

      stream.close();
   }
}
