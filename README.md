Intent Radio
============

Who might be interested?
------------------------

You might be interested in *Intent Radio* if:

- you use Android,
- you use Tasker,
- you listen to internet radio, and
- you're a geek.

What?
----

*Intent Radio* is an android internet radio app without a graphical user
interface.  It is controlled exclusively through the delivery of
[broadcast intents](http://developer.android.com/reference/android/content/BroadcastReceiver.html).
If you do not know what a broadcast intent is, then this is probably not the
app for you.

Download
--------

I haven't figured out the details of putting *Intent Radio* onto the Play
Store yet.  So, for the moment, the download is [here](http://smblott.org/intent_radio/).

Why?
----

There are already many internet radio apps for Android; so, why another
one?

Well, I couldn't find one that worked just right for me...

Xiialive::
   I tried (and like) [xiialaive](http://xiialive.com/).  And it supports external
   broadcast intents.  However, I was finding it would hang irredeemably
   on start up about two times in five, mainly when on mobile data.

Tunein::
   And I particularly like [tunein](http://tunein.com/).  However, it doesn't
   support either shortcuts or broadcast intents, so I have no way to
   start and stop it automatically, say when a headset is plugged in or
   out.

BBC IPlayer Radio::
   The
   [BBC iPlayer Radio](https://play.google.com/store/apps/details?id=uk.co.bbc.android.iplayerradio&hl=en)
   app is pretty slick; and most of what I listen to is BBC.  Again, however,
   there's no way to control playback without much pointy-pressy action through
   the GUI.

And then there's [Tasker](http://tasker.dinglisch.net/).  Tasker is an
automation app for Android.  It's like a small graphical programming
language combined with a mechanism to fire off tasks in response to various
events.

*Intent Radio* was written primarily to be driven by tasker, either via
task shortcuts on the home screen, or via Tasker's response to events such as
a headset being plugged in or out.

How?
----

*Intent Radio* supports the following broadcast intents...

`org.smblott.intentradio.PLAY`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
- start playback
- extras:
.. `url` -- the URL to play
.. `name` -- the display name for the station

Both extras are strings, and both are optional.  If `name` is omitted,
then the URL is used as the display name.  If `url` is omitted, then
a built-in URL for BBC Radio 4 is used.

`org.smblott.intentradio.STOP`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- stop playback
- extras: none

During playback, *Intent Radio* places a notification in the notification
area.  Clicking on the notification broadcasts the "`...STOP`" intent, which
causes playback to stop.

*Intent Radio* uses the built-in Android
[media player](http://developer.android.com/reference/android/media/MediaPlayer.html) for playback.  So all audio codecs supported natively by Android
are supported by *Intent Radio*.

Additionally, *Intent Radio* supports
[playlists](http://en.wikipedia.org/wiki/PLS_(file_format)) (whose URL must
end with the suffix `.pls`).  For example:

- `http://www.bbc.co.uk/.../xxx.pls`

// /////////////////////////////////////////////////////
== Warnings! ==

Although *Intent Radio* has no graphical user interface, you must
nevertheless start up the app *at least once*.  Otherwise, Android will not
deliver broadcast intents to the app.  This is an Android security feature.

Also, start up can be slow for some streams.  BBC Radio 4, for example,
takes in excess of 30 seconds for playback to begin.  I do not know the
source of this delay.  Please be patient.

Finally, *Intent Radio* is built for Android API level 16, so only for 4.1
(Jelly Bean) devices and above.

// /////////////////////////////////////////////////////
== A Sample Tasker Project ==

If you're using Tasker, then this [Tasker
project](https://github.com/smblott-github/intent_radio/tree/master/misc) may
be helpful in getting started with *Intent Radio*.

// /////////////////////////////////////////////////////
== Release Notes ==

Version 1.1
~~~~~~~~~~~

- Use `httpURLConnection`.
- Fetch playlists on an asynchronous thread (so, non-blocking).

Version 1.0
~~~~~~~~~~~

- Initial release.

