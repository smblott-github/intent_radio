package org.smblott.intentradio;

import android.content.Context;
import android.content.Intent;

public class State extends Logger
{
   private static Context context = null;
   private static String intent_state = null;

   public static final String STATE_STOP         = "stop";
   public static final String STATE_ERROR        = "error";
   public static final String STATE_COMPLETE     = "complete";
   public static final String STATE_PAUSE        = "play/pause";

   public static final String STATE_PLAY         = "play";
   public static final String STATE_BUFFER       = "play/buffering";
   public static final String STATE_DUCK         = "play/duck";
   public static final String STATE_DISCONNECTED = "play/disconnected";

   private static String current_state = STATE_STOP;
   private static boolean current_isNetworkUrl = false;

   public static void set_state(Context context, String s, boolean isNetworkUrl)
   {
      if ( s == null )
         return;

      if ( intent_state == null )
         intent_state = context.getString(R.string.intent_state);

      log("State.set_state(): ", s);
      current_state = s;
      current_isNetworkUrl = isNetworkUrl;
      Notify.note(isNetworkUrl);

      Intent intent = new Intent(intent_state);
      intent.putExtra("state", current_state);
      intent.putExtra("url", IntentPlayer.url);
      intent.putExtra("name", IntentPlayer.name);

      log("Broadcast: ", intent_state);
      log("Broadcast: state=", current_state);
      context.sendBroadcast(intent);
   }

   public static void get_state(Context context)
      { set_state(context, current_state, current_isNetworkUrl); }

   public static String current()
      { return current_state; }

   public static boolean is(String s)
      { return current_state.equals(s); }

   public static String text()
   {
      // is_stopped states.
      if ( is(STATE_STOP)         ) return "Stopped";
      if ( is(STATE_ERROR)        ) return "Error";
      if ( is(STATE_COMPLETE)     ) return "Complete";

      // is_playing states.
      if ( is(STATE_PLAY)         ) return "Playing";
      if ( is(STATE_BUFFER)       ) return "Buffering..";
      if ( is(STATE_DUCK)         ) return "Ducked";

      // paused.
      if ( is(STATE_PAUSE)        ) return "Paused..";

      // disconnected
      if ( is(STATE_DISCONNECTED) ) return "No network connection.";

      // Should not happen.
      //
      return "Unknown";
   }

   // These two predicates cover all states
   // except "pause".
   //
   public static boolean is_playing()
      { return is(State.STATE_PLAY) || is(State.STATE_BUFFER) || is(State.STATE_DUCK) || is(State.STATE_DISCONNECTED); }

   public static boolean is_stopped()
      { return State.is(State.STATE_STOP) || State.is(State.STATE_ERROR) || State.is(State.STATE_COMPLETE); }
}

