package org.smblott.intentradio;

import android.content.Intent;
import android.content.Context;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.TextUtils;
import java.util.List;
import android.os.AsyncTask;
import android.util.Log;

public class PlaylistPls extends Playlist
{
   PlaylistPls(Context ctx, String play_intent)
   {
      super(ctx,play_intent);
   }

   boolean goodLine(String line)
   {
      return line.startsWith("File");
   }
}
