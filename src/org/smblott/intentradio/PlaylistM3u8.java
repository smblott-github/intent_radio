package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistM3u8 extends Playlist
{
   PlaylistM3u8(IntentPlayer player)
      { super(player); }

   public static boolean is_playlist(String url)
      { return url.endsWith(".m3u") || url.endsWith(".m3u8"); }

   String filter(String line)
      { return line.indexOf('#') == 0 ? "" : line; }
}
