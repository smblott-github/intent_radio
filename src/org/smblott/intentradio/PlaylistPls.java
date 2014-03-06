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
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
         if ( ! lines.get(i).startsWith("File") )
            lines.set(i, "");

      ArrayList links = links(TextUtils.join("\n", lines));
      if ( links.size() == 0 )
         return null;

      return (String) links.get(new Random().nextInt(links.size()));
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
