
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

test:
	adb shell am broadcast -a org.zapto.smblott.intentradio.PLAY -e url mms://wmlive-nonacl.bbc.net.uk/wms/bbc_ami/radio4/radio4_bb_live_int_ep1_sl0 -e name "BBC Radio 4 (MMS)"

