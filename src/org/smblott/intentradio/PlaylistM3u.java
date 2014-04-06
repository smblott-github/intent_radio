package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistM3u extends Playlist
{
   PlaylistM3u(IntentPlayer player)
      { super(player); }

   public static boolean is_playlist(String url)
      { return url.endsWith(".m3u") || url.endsWith(".m3u8"); }

   String filter(String line)
      { return line.indexOf('#') == 0 ? "" : line; }
}
