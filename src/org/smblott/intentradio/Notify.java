package org.smblott.intentradio;

import java.lang.System;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;

import android.app.PendingIntent;

public class Notify
{
   private static final int note_id = 100;

   private static Service service = null;
   private static Context context = null;

   private static NotificationManager note_manager = null;
   private static Builder builder = null;
   private static Notification note = null;

   public static void init(Service a_service, Context a_context)
   {
      service = a_service;
      context = a_context;

      note_manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

      Intent click = new Intent(context, IntentPlayer.class).putExtra("action", service.getString(R.string.intent_click));
      PendingIntent pending_click = PendingIntent.getService(context, 0, click, 0);

      builder =
         new Notification.Builder(context)
            .setOngoing(false)
            .setSmallIcon(R.drawable.intent_radio)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(pending_click)
            .setContentTitle(service.getString(R.string.app_name_long))
            ;
   }

   private static String previous_state = null;
   private static String previous_name = null;
   private static boolean previous_foreground = false;

   public static void name(String name)
   {
      if ( previous_name == null || ! name.equals(previous_name) )
      {
         builder.setContentText(name);
         previous_name = name;
      }
   }

   public static void note()
   {
      builder.setWhen(System.currentTimeMillis());

      String state = State.text();
      if ( previous_state == null || ! state.equals(previous_state) )
      {
         Notification note = builder.setSubText(state+".").build();
         note_manager.notify(note_id, note);
         previous_state = state;
      }

      boolean current_foreground = State.is_playing();
      if ( current_foreground != previous_foreground )
      {
         if ( current_foreground )
         {
            log("Starting foreground.");
            builder.setContentInfo("(Touch to stop)");
            Notification note = builder.setPriority(Notification.PRIORITY_HIGH).build();
            service.startForeground(note_id, note);
         }
         else
         {
            log("Stopping foreground.");
            service.stopForeground(true);
            builder.setContentInfo("(Touch to restart)");
            Notification note = builder.setPriority(Notification.PRIORITY_DEFAULT).build();
            note_manager.notify(note_id, note);
         }
         previous_foreground = current_foreground;
      }
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }

   private static void toast(String msg)
      { Logger.toast(msg); }

}
