package org.smblott.intentradio;

import android.content.Context;
import android.content.Intent;

public class State extends Logger
{
   private static Context context = null;
   private static String intent_state = null;

   public static final String STATE_STOP   = "stop";
   public static final String STATE_PLAY   = "play";
   public static final String STATE_BUFFER = "play/buffering";
   public static final String STATE_PAUSE  = "play/pause";
   public static final String STATE_DIM    = "play/dim";
   public static final String STATE_ERROR  = "error";

   private static String current_state = STATE_STOP;

   private static boolean init_strings(Context context)
   {
      if ( context != null )
      {
         if ( intent_state == null )
            intent_state = context.getString(R.string.intent_state);
         log("State.init_strings(): ok.");
      }
      else
         log("State.init_strings(): error: context not set,");

      return context != null;
   }

   public static void set_state(Context context, String s)
   {
      if ( s == null || ! init_strings(context) )
         return;

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

   public static boolean is(String s)
      { return current_state.equals(s); }
}

