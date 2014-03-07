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
      List<String> lines = new ArrayList<String>();
      HttpURLConnection conn = null;

      try {
         URL url = new URL(str);
         conn = (HttpURLConnection) url.openConnection();
         InputStream in = new BufferedInputStream(conn.getInputStream());
         readStream(in, lines);
      }
      catch ( Exception e ) {
         if ( conn != null )
            conn.disconnect();
      }

      return lines;
   }

   private static void readStream(InputStream in, List<String> lines) throws Exception
   {
      BufferedReader fd = new BufferedReader(new InputStreamReader(in));
      String line = null;

      while ((line = fd.readLine()) != null)
         lines.add(line);

      in.close();
   }
}
