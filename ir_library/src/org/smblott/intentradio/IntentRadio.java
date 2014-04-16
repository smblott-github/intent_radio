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

import android.content.ClipData;
import android.content.ClipboardManager;

public class IntentRadio extends Activity
{
   private static Context context = null;

   private AsyncTask<Object, Void, Spanned> draw_task = null;
   private AsyncTask<Void, Void, String> install_task = null;
   private String url = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      context = getApplicationContext();
      Logger.init(context);

      // Handle app activity...
      //
      draw_task = null;
      install_task = null;

      setContentView(R.layout.main);

      TextView view = (TextView) findViewById(R.id.text);
      view.setMovementMethod(LinkMovementMethod.getInstance());
      view.setText("Loading...");

      // Read file contents and build date for main screen asyncronously...
      //
      draw_task = new AsyncTask<Object, Void, Spanned>()
      {
         private TextView view = null;
         private Integer id = null;
         private String url = null;

         @Override
         protected Spanned doInBackground(Object... args)
         {
            view = (TextView) args[0];
            id = (Integer) args[1];
            url = (String) args[2];

            String text = ReadRawTextFile.read(getApplicationContext(),id.intValue());
            if ( url != null )
               text = text.replace("REPLACE_URL", url);

            return Html.fromHtml(
                    text 
                  + "<p>\n"
                  + "Distribution: " + getString(R.string.distribution) + "<br>\n"
                  + "Version: " + getString(R.string.version) + "<br>\n" 
                  + "Build: " + Build.getBuildDate(context) + "\n"
                  + "</p>\n"
                  );
         }

         @Override
         protected void onPostExecute(Spanned html)
         {
            if ( ! isCancelled() )
               view.setText(html);
         }

      };

      // Handle intent...
      //
      Intent intent = getIntent();
      String action = intent.getAction();
      if ( action.equals(Intent.ACTION_VIEW) )
      {
         url = intent.getDataString();
         Intent msg = new Intent(context, IntentPlayer.class);
         msg.putExtra("action", getString(R.string.intent_play));
         msg.putExtra("url", url);
         context.startService(msg);
         findViewById(R.id.clip_url).setVisibility(View.VISIBLE);
         draw_task.execute(view, R.raw.playing, url);
         return;
      }

      // Open app...
      //
      findViewById(R.id.clip_buttons).setVisibility(View.VISIBLE);
      findViewById(R.id.install_tasker).setVisibility(View.VISIBLE);
      draw_task.execute(view, R.raw.message, null);
   }

   /* ********************************************************************
    * Destroy activity: clean up any remaining tasks...
    */

   public void onDestroy()
   {
      if ( draw_task != null && draw_task.getStatus() != AsyncTask.Status.FINISHED )
         draw_task.cancel(true);

      if ( install_task != null && install_task.getStatus() != AsyncTask.Status.FINISHED )
         install_task.cancel(true);

      draw_task = null;
      install_task = null;

      super.onDestroy();
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
      if ( install_task != null && install_task.getStatus() != AsyncTask.Status.FINISHED )
         return;

      install_task = new AsyncTask<Void, Void, String>()
      {
         @Override
         protected String doInBackground(Void... unused)
         {
            return CopyResource.copy(context, R.raw.tasker, project_file);
         }

         @Override
         protected void onPostExecute(String error)
         {
            if ( isCancelled() )
               return;

            if ( error == null /* so, success */ )
            {
               toast("Project file installed...\n\n/sdcard/" + project_file);
               toast("Next, import this project into Tasker.");
            }
            else
               toast("Install error:\n" + error + "\n\n/sdcard/" + project_file);
         }

      };
      install_task.execute();
   }

   /* ********************************************************************
    * Clip url...
    */

   public void clip_url(View view)
      { Clipper.clip(context,url); }

   /* ********************************************************************
    * Toasts...
    */

   static private void toast(String msg)
      { Logger.toast_long(msg); }

}
