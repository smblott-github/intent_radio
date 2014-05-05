package org.smblott.intentradio;

import android.app.Activity;
import android.content.Intent;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

public abstract class PreferenceActivity extends Activity
{
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.prefs, menu);
      return true;
   } 

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      if ( item.getItemId() == R.id.prefs )
      {
         startActivity(new Intent(this, Prefs.class));
         return true;   
      }

      return super.onOptionsItemSelected(item);
   }
}
