package org.smblott.intentradio;

import android.content.Context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ReadRawTextFile
{
   /* ********************************************************************
    * Read raw text file resource...
    *
    * source: http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
    */

   public static String read(Context context, int resId)
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
}
