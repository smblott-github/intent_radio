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
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.text.method.LinkMovementMethod;

public class IntentRadio extends Activity
{

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Logger.init(getApplicationContext());

      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      TextView text = (TextView) findViewById(R.id.text);
      text.setMovementMethod(LinkMovementMethod.getInstance());
      text.setText(Html.fromHtml(readRawTextFile(getApplicationContext(), R.raw.message)));

      Button button = (Button) findViewById(R.id.clip_buttons);
      button.setOnClickListener( new Button.OnClickListener() {
               public void onClick(View v) {
                  Intent c = new Intent(IntentRadio.this, ClipButtons.class);
                  startActivity(c);
               } } );
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

   public void clip_buttons()
   {
      Logger.log("Clip buttons.");
      // Intent c = new Intent(IntentRadio.this, ClipButtons.class);
      // startActivity(c);
   }

}
