package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistM3u extends Playlist
{
   public static String suffix = ".m3u";

   PlaylistM3u(Context context, String play_intent)
      { super(context,play_intent); }

   String filter(String line)
      { return line.indexOf('#') == 0 ? "" : line; }
}
