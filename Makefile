
build:
	$(MAKE) debug

debug: res/raw/tasker.prj
	ant debug

release: res/raw/tasker.prj
	ant release

res/raw/tasker.prj: misc/Radio.prj.xml
	install -m 0444 $< $@

clean:
	ant clean

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

install-release:
	$(MAKE) release
	adb install -r bin/IntentRadio-release.apk

play:
	adb shell am broadcast -a org.smblott.intentradio.PLAY -e debug yes

stop:
	adb shell am broadcast -a org.smblott.intentradio.STOP -e debug yes

ufm:
	adb shell am broadcast -a org.smblott.intentradio.PLAY -e url http://192.168.3.3/cgi-bin/sc/wav -e name "Elsa Sound Card" -e debug yes

wnyc:
	adb shell am broadcast -a org.smblott.intentradio.PLAY -e url http://www.wnyc.org/stream/fm.pls -e name "WNYC" -e debug yes

newstalk:
	adb shell am broadcast -a org.smblott.intentradio.PLAY -e url http://communicorp.mp3.miisolutions.net:8000/communicorp/Newstalk_low.m3u -e name "Newstalk" -e debug yes

lyric:
	adb shell am broadcast -a org.smblott.intentradio.PLAY -e url http://icecast2.rte.ie/lyric -e name "RTE Lyric FM" -e debug yes

view:
	adb shell am broadcast -a android.intent.action.VIEW -d http://icecast2.rte.ie/lyric

pause:
	adb shell am broadcast -a org.smblott.intentradio.PAUSE -e debug yes

restart:
	adb shell am broadcast -a org.smblott.intentradio.RESTART -e debug yes

logcat:
	adb logcat -s IntentRadio -s MediaPlayer

log:
	$(MAKE) logcat

log_file:
	adb shell cat /sdcard/Android/data/org.smblott.intentradio/files/intent-radio.log

log_file_tail:
	$(MAKE) log_file | tail -40

update-project:
	android update project -p .

# Release process:
#
# - bump
#   ** version code in ./AndroidManifest.xml
#   ** version name in ./res/values/strings.xml
# - update release notes in web/index.ascii
# - git commit/push
# - build release APK
# - git tag -a vX.Y
# - git push origin --tags
# - on GitHub, publish release
#   including upload of release APK
# - add link to release APK to web/index.ascii (at bottom)
# - in web: make install
# - git commit/push
#

