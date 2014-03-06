package org.smblott.intentradio;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.TextUtils;
import java.util.List;

public class PlaylistPls
{

   public static String get(String url)
   {
      Random random = new Random();
      int i;

      List<String> text = HttpGetter.httpGet(url);
      for (i=0; i<text.size(); i+= 1)
         if ( ! text.get(i).startsWith("File") && 0 <= text.get(i).indexOf('=') )
            text.set(i, "");

      ArrayList links = links(TextUtils.join("\n", text));
      if ( links.size() == 0 )
         return null;

      return (String) links.get(random.nextInt(links.size()));
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
