package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistPls extends Playlist
{
   public static String suffix = ".pls";

   PlaylistPls(Context context, String play_intent)
      { super(context,play_intent); }

   String filter(String line)
   {
      if ( line.startsWith("File") && 0 < line.indexOf('=') )
         return line;

      return "";
   }
}
