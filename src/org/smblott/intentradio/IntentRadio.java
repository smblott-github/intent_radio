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

      TextView view = (TextView) findViewById(R.id.text);
      view.setMovementMethod(LinkMovementMethod.getInstance());
      view.setText("Loading...");

      // Read file contents and build date for main screen asyncronously...
      //
      new AsyncTask<TextView, Void, Spanned>()
      {
         TextView view = null;

         @Override
         protected Spanned doInBackground(TextView... v)
         {
            view = v[0];
            String file = ReadRawTextFile.read(getApplicationContext(),R.raw.message);

            String version = getString(R.string.version);
            version = "<p>Version: " + version + "<br>\n";

            String build_date = Build.getBuildDate(context);
            build_date = "Build: " + build_date + "\n</p>\n";

            return Html.fromHtml(file + version + build_date );
         }

         @Override
         protected void onPostExecute(Spanned html)
            { view.setText(html); }

      }.execute(view);
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
    *
    * This currently assumes that Tasker *always* stores projects in:
    *
    *    - /sdcard/Tasker/projects
    *
    * Does it?
    *
    * File I/O is more blocking than anything else we're doing, so we'll do it
    * asyncronously.
    */

   private static final String project_file = "Tasker/projects/IntentRadio.prj.xml";

   public void install_tasker(View v)
   {

      new AsyncTask<Void, Void, String>()
      {
         @Override
         protected String doInBackground(Void... unused)
         {
            return CopyResource.copy(context, R.raw.tasker, project_file);
         }

         @Override
         protected void onPostExecute(String error)
         {
            if ( error == null )
            {
               toast("Project file installed...\n\n/sdcard/" + project_file);
               toast("Now import this project into Tasker.");
            }
            else
               toast("Install error:\n" + error + "\n\n/sdcard/" + project_file);
         }

      }.execute();
   }

   /* ********************************************************************
    * Toasts...
    */

   static private void toast(String msg)
      { Logger.toast_long(msg); }

}
