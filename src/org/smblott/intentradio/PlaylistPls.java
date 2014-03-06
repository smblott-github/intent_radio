package org.smblott.intentradio;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PlaylistPls
{

   public static String get(String url)
   {
      Random random = new Random();
      String text = HttpGetter.httpGetStr(url);
      ArrayList urls = links(text);
      if ( urls.size() == 0 )
         return null;

      return (String) urls.get(random.nextInt(urls.size()));
   }

   // source: http://blog.houen.net/java-get-url-from-string/
   //
   private static ArrayList links(String text)
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
