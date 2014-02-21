package org.zapto.smblott.intentradio;

import android.app.Activity;
import android.os.Bundle;

import android.content.Context;
import android.content.Intent;

import android.widget.TextView;
import android.content.BroadcastReceiver;

public class IntentRadio extends Activity
{
   private static String intent_log = null;
   private static TextView text_view = null;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      intent_log = getString(R.string.intent_log);
      text_view = (TextView) findViewById(R.id.log);

      // This isn't working...
      BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
         {
            @Override
            public void onReceive(Context context, Intent intent)
            {
               if ( intent_log.equals(intent.getAction()) && intent.hasExtra("msg") )
                  log(intent.getStringExtra("msg"));      
            }
         };

      setContentView(R.layout.main);
   }

   private void log(String msg)
   {
      text_view.append("\n" + msg);
   }

}
