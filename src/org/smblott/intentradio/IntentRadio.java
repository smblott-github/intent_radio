package org.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

import android.content.Context;
import android.content.Context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.content.ClipData;
import android.content.ClipboardManager;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntentRadio extends Activity
{

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      Context context = getApplicationContext();
      Logger.init(context);

      setContentView(R.layout.main);

      String version = getString(R.string.version);
      version = "<p>Version: " + version + "<br>\n";

      String build_date = Build.getBuildDate(context);
      build_date = "Build: " + build_date + "\n</p>\n";

      String file = readRawTextFile(getApplicationContext(),R.raw.message);
      Spanned html = Html.fromHtml(file + version + build_date );

      TextView text = (TextView) findViewById(R.id.text);
      text.setMovementMethod(LinkMovementMethod.getInstance());
      text.setText(html);
   }

   /* ********************************************************************
    * Read raw text file resource...
    *
    * source: http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
    */

   private static String readRawTextFile(Context context, int resId)
   {
      InputStream inputStream = context.getResources().openRawResource(resId);
      InputStreamReader inputreader = new InputStreamReader(inputStream);
      BufferedReader buffreader = new BufferedReader(inputreader);

      String line;
      StringBuilder text = new StringBuilder();

      try {
         while ( ( line = buffreader.readLine()) != null )
            text.append(line + "\n" );
      }
      catch (Exception e)
         { return ""; }

      return text.toString();
   }

   /* ********************************************************************
    * Clip buttons...
    */

   public void clip_buttons(View v)
   {
      Intent c = new Intent(IntentRadio.this, ClipButtons.class);
      startActivity(c);
   }

}
