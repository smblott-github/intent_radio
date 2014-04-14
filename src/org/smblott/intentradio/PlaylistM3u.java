package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;
import android.net.Uri;

public class PlaylistM3u extends Playlist
{
   PlaylistM3u(IntentPlayer player)
      { super(player); }

   public static boolean is_playlist(String url)
      { return is_playlist_suffix(url,".m3u") || is_playlist_suffix(url,".m3u8"); }

   @Override
   String filter(String line)
      { return line.indexOf('#') == 0 ? "" : line; }
}
