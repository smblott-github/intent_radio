package org.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

import android.content.Context;

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

      setContentView(R.layout.main);

      String version = getString(R.string.version);
      version = "<p>Version: " + version + "<br>\n";

      String build_date = Build.getBuildDate(context);
      build_date = "Build: " + build_date + "\n</p>\n";

      String file = ReadRawTextFile.read(getApplicationContext(),R.raw.message);
      Spanned html = Html.fromHtml(file + version + build_date );

      TextView view = (TextView) findViewById(R.id.text);
      view.setMovementMethod(LinkMovementMethod.getInstance());
      view.setText(html);
   }

   /* ********************************************************************
    * Launch clip buttons...
    */

   public void clip_buttons(View v)
   {
      Intent clipper = new Intent(IntentRadio.this, ClipButtons.class);
      startActivity(clipper);
   }
}
