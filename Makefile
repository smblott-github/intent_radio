
debug:
	ant debug

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

play:
	adb shell am broadcast -a org.zapto.smblott.intentradio.PLAY

stop:
	adb shell am broadcast -a org.zapto.smblott.intentradio.STOP

logcat:
	adb logcat -s IntentRadio
