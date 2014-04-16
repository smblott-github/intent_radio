package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistPls extends Playlist
{
   PlaylistPls(IntentPlayer player)
      { super(player); }

   public static boolean is_playlist(String url)
      { return is_playlist_suffix(url,".pls"); }

   @Override
   String filter(String line)
   {
      if ( line.startsWith("File") && 0 < line.indexOf('=') )
         return line;

      return "";
   }
}
