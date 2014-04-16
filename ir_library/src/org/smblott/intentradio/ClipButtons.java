package org.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;

import android.content.Context;

import android.view.View;
import android.widget.Button;

public class ClipButtons extends Activity
{

   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;
   private static String intent_state_request = null;
   private static String intent_state = null;

   private static Context context = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      Logger.init(getApplicationContext());
      context = getApplicationContext();

      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      intent_pause = getString(R.string.intent_pause);
      intent_restart = getString(R.string.intent_restart);
      intent_state_request = getString(R.string.intent_state_request);
      intent_state = getString(R.string.intent_state);

      setContentView(R.layout.buttons);
   }

   /* ********************************************************************
    * Clip buttons...
    */

   public static void clip_play(View view)          { Clipper.clip(context,intent_play); }
   public static void clip_stop(View view)          { Clipper.clip(context,intent_stop); }
   public static void clip_pause(View view)         { Clipper.clip(context,intent_pause); }
   public static void clip_restart(View view)       { Clipper.clip(context,intent_restart); }
   public static void clip_state_request(View view) { Clipper.clip(context,intent_state_request); }
   public static void clip_state(View view)         { Clipper.clip(context,intent_state); }

   /* ********************************************************************
    * Utilities...
    */

   private static void toast(String msg)
      { Logger.toast(msg); }
}
