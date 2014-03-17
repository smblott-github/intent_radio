package org.smblott.intentradio;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;

import android.os.AsyncTask;;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntentRadio extends Activity
{
   static Context context = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      context = getApplicationContext();
      Logger.init(context);

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

   /* ********************************************************************
    * Install sample Tasker project...
    */

   public void install_tasker(View v)
   {
      new AsyncTask<Void, Void, String>()
      {
         protected String doInBackground(Void... params)
         {
            return CopyResource.copy(context,R.raw.tasker, "Tasker/projects/IntentRadio.prj.xml");
         }

         protected void onPostExecute(String path)
         {
            path = path != null ? path : "Unknown error.";

            if ( path.indexOf('/') == 0 )
            {
               toast("Project file installed:\n" + path);
               toast("Now import this project into Tasker.");
            }
            else
               toast(path);
         }

      }.execute();
   }

   /* ********************************************************************
    * Toasts...
    */

   static private void toast(String msg)
      { Logger.toast_long(msg); }

}
