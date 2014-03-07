package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

public class PlaylistPls extends Playlist
{
   PlaylistPls(Context context, String play_intent)
   {
      super(context,play_intent);
   }

   boolean keep(String line)
   {
      return line.startsWith("File");
   }
}
