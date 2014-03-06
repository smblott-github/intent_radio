package org.smblott.intentradio;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

import java.util.List;
import java.util.LinkedList;
import android.text.TextUtils;

public class HttpGetter
{
   public static List<String> httpGet(String str)
   {
      List<String> lines = new LinkedList<String>();
      HttpURLConnection conn;

      try {
         URL url = new URL(str);
         conn = (HttpURLConnection) url.openConnection();
      }
      catch ( Exception e ) {
         return lines;
      }

      try {
         InputStream in = new BufferedInputStream(conn.getInputStream());
         lines = readStream(in, lines);
      }
      catch ( Exception e ) {
      }
      finally {
         conn.disconnect();
      }

      return lines;
   }

   public static String httpGetStr(String str)
   {
      return TextUtils.join("\n", httpGet(str));
   }

   private static List<String> readStream(InputStream in, List<String> lines) throws Exception
   {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = null;

      while ((line = reader.readLine()) != null)
         lines.add(line);

      in.close();
      return lines;
   }

}
