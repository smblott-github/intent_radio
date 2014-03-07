package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistPls extends Playlist
{
   public static String suffix = ".pls";

   PlaylistPls(Context context, String play_intent)
      { super(context,play_intent); }

   boolean keep(String line)
      { return line.startsWith("File") && 0 < line.indexOf('='); }
}
