package org.smblott.intentradio;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

import android.util.Log;
import java.util.List;
import java.util.LinkedList;
import android.text.TextUtils;

public class HttpGetter
{
   public static List<String> httpGet(String str)
   {
      List<String> lines = new LinkedList<String>();
      HttpURLConnection conn;

      log("> " + str);
      try {
         URL url = new URL(str);
         conn = (HttpURLConnection) url.openConnection();
         log(conn.toString());
      }
      catch ( Exception e ) { return lines; }

      try {
         log("4");
         InputStream in = new BufferedInputStream(conn.getInputStream());
         log("5");
         lines = readStream(in, lines);
         log("6");
      }
      catch ( Exception e ) { Log.e("IntentRadio", "exception", e); }
      finally { conn.disconnect(); }

         log("7");
      return lines;
   }

   public static String httpGetStr(String str)
   {
      return TextUtils.join("\n", httpGet(str));
   }

   private static List<String> readStream(InputStream in, List<String> lines) throws Exception
   {
      log("41");
      BufferedReader fd = new BufferedReader(new InputStreamReader(in));
      log("42");
      String line = null;
      log("43");

      while ((line = fd.readLine()) != null)
      {
         log("44");
         log(line);
         lines.add(line);
         log("45");
      }

      log("46");
      in.close();
      return lines;
   }

   private static void log(String msg)
   {
      if ( msg != null )
         Log.d("IntentRadio", msg);
   }
}
