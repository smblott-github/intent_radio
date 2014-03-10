package org.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;

import android.content.Context;
import android.content.Context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.content.ClipData;
import android.content.ClipboardManager;

import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.text.method.LinkMovementMethod;
import android.widget.Button;

public class ClipButtons extends Activity
{

   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;

   private static ClipboardManager clip_manager = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Logger.init(getApplicationContext());

      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      intent_pause = getString(R.string.intent_pause);
      intent_restart = getString(R.string.intent_restart);

      clip_manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

      super.onCreate(savedInstanceState);
      setContentView(R.layout.buttons);

      /*
      setText(R.id.clip_play, intent_play);
      setText(R.id.clip_stop, intent_stop);
      setText(R.id.clip_pause, intent_pause);
      setText(R.id.clip_restart, intent_restart);
      */
   }

   /* ********************************************************************
    * Clip buttons...
    */

   /*
   private void setText(int id, String txt)
   {
      Button button = (Button) findViewById(id);
      button.setText("Copy " + txt);
   }
   */

   private static void clip(String text)
   {
      ClipData clip_data = ClipData.newPlainText("text", text);
      clip_manager.setPrimaryClip(clip_data);
      toast("Clipboard:\n" + text);
   }

   public static void clip_play(View view)    { clip(intent_play); }
   public static void clip_stop(View view)    { clip(intent_stop); }
   public static void clip_pause(View view)   { clip(intent_pause); }
   public static void clip_restart(View view) { clip(intent_restart); }

   /* ********************************************************************
    * Utilities...
    */

   private static void toast(String msg)
      { Logger.toast(msg); }
}
