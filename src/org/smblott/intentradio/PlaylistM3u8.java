package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistM3u8 extends Playlist
{
   public static String suffix = ".m3u8";

   PlaylistM3u8(IntentPlayer player)
      { super(player); }

   String filter(String line)
      { return line.indexOf('#') == 0 ? "" : line; }
}
