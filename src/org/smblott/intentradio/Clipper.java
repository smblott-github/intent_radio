package org.smblott.intentradio;

import android.content.Context;

import android.content.ClipData;
import android.content.ClipboardManager;

public class Clipper extends Logger
{
   static void clip(Context context, String text)
   {
      if ( text == null || context == null )
         return;

      Logger.init(context);

      ClipboardManager clip_manager = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
      ClipData clip_data = ClipData.newPlainText("text", text);
      clip_manager.setPrimaryClip(clip_data);
      toast("Clipboard:\n" + text);
   }
}
