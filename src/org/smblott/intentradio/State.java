package org.smblott.intentradio;

import android.content.Context;
import android.content.Intent;

public class State extends Logger
{
   private static Context context = null;
   private static String intent_state = null;

   public static final String STATE_STOP   = "stop";
   public static final String STATE_ERROR  = "error";
   public static final String STATE_PAUSE  = "play/pause";

   public static final String STATE_PLAY   = "play";
   public static final String STATE_BUFFER = "play/buffering";
   public static final String STATE_DIM    = "play/dim";

   private static String current_state = STATE_STOP;

   public static void set_state(Context context, String s)
   {
      if ( s == null )
         return;

      if ( intent_state == null )
         intent_state = context.getString(R.string.intent_state);

      log("State.set_state(): ", s);
      current_state = s;

      Intent intent = new Intent(intent_state);
      intent.putExtra("state", current_state);
      intent.putExtra("url", IntentPlayer.url);
      intent.putExtra("name", IntentPlayer.name);

      log("Broadcast: ", intent_state);
      log("Broadcast: state=", current_state);
      context.sendBroadcast(intent);
   }

   public static void get_state(Context context)
      { set_state(context, current_state); }

   public static String current()
      { return current_state; }

   public static boolean is(String s)
      { return current_state.equals(s); }

   public static boolean is_playing()
      { return is(State.STATE_PLAY) || is(State.STATE_BUFFER) || is(State.STATE_DIM); }
}

