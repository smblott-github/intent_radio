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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.Header;

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

         InputStream stream = new BufferedInputStream(connection.getInputStream());
         readStream(stream, lines);
      }
      catch ( Exception e )
         { if ( connection != null ) connection.disconnect(); }

      return lines;
   }

   /*
    * Not being used.
    *
    * It seems that the MIME type delivered in response to a HEAD request is
    * not a reliable way of determining the actual content type, in practice.
    *
   public static String httpMime(String str)
   {
      HttpClient httpclient = new DefaultHttpClient();
      HttpHead httphead = new HttpHead(str);

      try
      {
         HttpResponse response = httpclient.execute(httphead);

         if ( response.containsHeader("Content-Type") )
            return response.getHeaders("Content-Type")[0].getValue();
         // TODO:
         // How to we clean up/close this connection?
      }
      catch ( Exception e )
         {} // { if ( connection != null ) connection.disconnect(); }

      return "";
   }
   */

   // public static String httpMime(String str)
   // {
   //    HttpURLConnection connection = null;

   //    try
   //    {
   //       URL url = new URL(str);
   //       connection = new HttpURLConnection(url);
   //       connection.setRequestMethod("HEAD");
   //       connection.openConnection();
   //       String mime = connection.getContentType();
   //       connection.disconnect();
   //       return mime;
   //    }
   //    catch ( Exception e )
   //       { if ( connection != null ) connection.disconnect(); }

   //    return "";
   // }

   private static void readStream(InputStream stream, List<String> lines) throws Exception
   {
      String line;
      BufferedReader buff = new BufferedReader(new InputStreamReader(stream));

      while ((line = buff.readLine()) != null)
         lines.add(line);

      stream.close();
   }
}
