
build:
	$(MAKE) debug

debug:
	ant debug

release:
	ant release

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

pause:
	adb shell am broadcast -a org.smblott.intentradio.PAUSE -e debug yes

restart:
	adb shell am broadcast -a org.smblott.intentradio.RESTART -e debug yes

logcat:
	adb logcat -s IntentRadio

log:
	$(MAKE) logcat

log_file:
	adb shell cat /mnt/shell/emulated/0/Android/data/org.smblott.intentradio/files/intent-radio.log

update-project:
	android update project -p .

