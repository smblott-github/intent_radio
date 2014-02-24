
build:
	$(MAKE) debug

debug:
	ant debug

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

play:
	adb shell am broadcast -a org.zapto.smblott.intentradio.PLAY

stop:
	adb shell am broadcast -a org.zapto.smblott.intentradio.STOP

ufm:
	adb shell am broadcast -a org.zapto.smblott.intentradio.PLAY -e url http://192.168.3.3/cgi-bin/sc/wav -e name "Elsa Sound Card"

lyric:
	adb shell am broadcast -a org.zapto.smblott.intentradio.PLAY -e url http://icecast2.rte.ie/lyric -e name "RTE Lyric FM"

logcat:
	adb logcat -s IntentRadio

log:
	$(MAKE) logcat

log_file:
	adb shell cat /mnt/shell/emulated/0/Android/data/org.zapto.smblott.intentradio/files/intent-radio.log
