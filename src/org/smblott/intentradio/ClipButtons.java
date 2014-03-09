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

public class ClipButtons extends Activity
{

   private static String intent_play = null;
   private static String intent_stop = null;
   private static String intent_pause = null;
   private static String intent_restart = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Logger.init(getApplicationContext());

      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);
      intent_pause = getString(R.string.intent_pause);
      intent_restart = getString(R.string.intent_restart);

      super.onCreate(savedInstanceState);
      setContentView(R.layout.buttons);
   }

   /* ********************************************************************
    * Clip buttons...
    */

   private void clip(String text)
   {
      ClipboardManager clip_manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

      ClipData clip_data = ClipData.newPlainText("text", text);
      clip_manager.setPrimaryClip(clip_data);
      toast("Clipboard:\n" + text);
   }

   public void clip_play(View view)
      { clip(intent_play); }

   public void clip_stop(View view)
      { clip(intent_stop); }

   public void clip_pause(View view)
      { clip(intent_pause); }

   public void clip_restart(View view)
      { clip(intent_restart); }

   /* ********************************************************************
    * Utilities...
    */

   private void toast(String msg)
      { Logger.toast(msg); }
}
