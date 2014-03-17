package org.smblott.intentradio;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CopyResource extends Logger
{
   // source: http://stackoverflow.com/questions/8664468/copying-raw-file-into-sdcard
   //
   public static boolean copy(Context context, int id, String path)
   {
      log("CopyResource id:", ""+id);
      log("CopyResource path:", path);

      byte[] buff = new byte[1024];
      int read = 0;
      boolean success = true;

      InputStream in = null;
      FileOutputStream out = null;

      File sdcard = Environment.getExternalStorageDirectory();
      if ( sdcard == null )
      {
         toast("Error: SD card path not found.");
         return false;
      }

      path = sdcard.getAbsolutePath() + "/" + path;
      log("CopyResource path: ", path);
      File file = new File(path);
      File directory = new File(file.getParent());

      if ( ! directory.isDirectory() )
      {
         toast("Parent directory does not exist:\n" + file.getParent());
         return false;
      }

      if ( file.exists() )
      {
         toast("Error: file exists!\n" + path);
         return false;
      }

      try
      {
         in = context.getResources().openRawResource(id);
         out = new FileOutputStream(path);

         while ((read = in.read(buff)) > 0)
            out.write(buff, 0, read);
      }
      catch ( Exception e)
         { success = false; toast("Failed to copy file:\n" + path); }
      finally
      {
         if ( success )
            toast_long("File installed:\n" + path);

         try
         {
            if ( in  != null ) in.close();
            if ( out != null ) out.close();
         }
         catch ( Exception e) {}
      }

      return success;
   }
}
