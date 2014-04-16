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
   private static boolean use_broadcast_intent = true;

   public static void init(Service a_service, Context a_context)
   {
      service = a_service;
      context = a_context;

      note_manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

      PendingIntent pending_click = null;
      String intent_click = context.getString(R.string.intent_click);
      if ( use_broadcast_intent )
      {
         // Either:
         // Use broadcast to deliver clicks...
         log("Notify: using broadcasts to deliver clicks.");
         Intent click = new Intent(intent_click);
         pending_click = PendingIntent.getBroadcast(context, 0, click, 0);
      }
      else
      {
         // Or:
         // Use service class to deliver clicks...
         //
         // Android seems to get confused if you use a pending intent to a service
         // class, and two apps (with different packages) that use
         // the *same* service class name are installed.
         // This could be a security issue.
         //
         log("Notify: using service to deliver clicks.");
         Intent click = new Intent(context, IntentPlayer.class);
         click.putExtra("action", intent_click);
         pending_click = PendingIntent.getService(context, 0, click, 0);
      }

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
   private static boolean notification_created = false;

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
      // Note:
      // STATE_PAUSED is *not* in the foreground.
      //
      boolean current_foreground = State.is_playing();

      if ( current_foreground != previous_foreground || ! notification_created )
      {
         if ( current_foreground )
         {
            log("Starting foreground.");
            Notification note =
               builder
                  .setContentInfo("(touch to stop)")
                  .setOngoing(true)
                  .setPriority(Notification.PRIORITY_HIGH)
                  .setWhen(System.currentTimeMillis())
                  .build();
            service.startForeground(note_id, note);
         }
         else
         {
            log("Stopping foreground.");
            // It would be nice to use "false", below.  However, while that
            // gives nice smooth notification transitions, the resulting
            // notification is *always* "ongoing", so it cannot be dismissed.
            //
            service.stopForeground(true);
            Notification note =
               builder
                  .setContentInfo("(touch to restart)")
                  .setOngoing(false)
                  .setPriority(Notification.PRIORITY_DEFAULT)
                  .setWhen(System.currentTimeMillis())
                  .build();
            note_manager.notify(note_id, note);
         }
         previous_foreground = current_foreground;
         notification_created = true;
      }

      String state = State.text();
      if ( previous_state == null || ! state.equals(previous_state) )
      {
         log("Notify state: ", state);
         Notification note =
            builder
            .setSubText(state+".")
            .setWhen(System.currentTimeMillis())
            .build();
         note_manager.notify(note_id, note);
         previous_state = state;
      }
   }

   public static void cancel()
   {
      log("Notify cancel().");
      note_manager.cancelAll();
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }

   private static void toast(String msg)
      { Logger.toast(msg); }

}
