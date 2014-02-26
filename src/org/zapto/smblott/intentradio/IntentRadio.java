package org.zapto.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;

import android.content.Context;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import android.widget.Toast;
import android.view.View;
import android.content.Context;

import android.content.ClipData;
import android.content.ClipboardManager;

public class IntentRadio extends Activity
{

   // http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
   public static String readRawTextFile(Context ctx, int resId)
   {
      InputStream inputStream = ctx.getResources().openRawResource(resId);

      InputStreamReader inputreader = new InputStreamReader(inputStream);
      BufferedReader buffreader = new BufferedReader(inputreader);

      String line;
      StringBuilder text = new StringBuilder();

      try {
         while (( line = buffreader.readLine()) != null)
         {
            text.append(line);
            text.append('\n');
         }
      } catch (Exception e)
      {
         return null;
      }

      return text.toString();
   }

   private static String intent_play = null;
   private static String intent_stop = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      intent_play = getString(R.string.intent_play);
      intent_stop = getString(R.string.intent_stop);

      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      TextView text = (TextView)findViewById(R.id.text);
      text.setMovementMethod(new ScrollingMovementMethod());
      text.setText(readRawTextFile(getApplicationContext(), R.raw.message));
   }

   /* ********************************************************************
    * Buttons...
    */

   private void clip(String text)
   {
      ClipboardManager clip_manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

      ClipData clip_data = ClipData.newPlainText("text", text);
      clip_manager.setPrimaryClip(clip_data);
      toast("Clipped:\n" + text);
   }

   public void clip_play(View view)
   {
      clip(intent_play);
   }

   public void clip_stop(View view)
   {
      clip(intent_stop);
   }

   private void toast(String msg)
   {
      Context context = getApplicationContext();
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
   }

}
