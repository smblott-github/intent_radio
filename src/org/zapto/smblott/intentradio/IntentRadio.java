package org.zapto.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;

import android.content.Context;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

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

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      TextView text = (TextView)findViewById(R.id.text);
      text.setMovementMethod(new ScrollingMovementMethod());
      text.setText(readRawTextFile(getApplicationContext(), R.raw.message));
   }

}
