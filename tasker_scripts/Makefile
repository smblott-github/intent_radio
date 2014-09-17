
dest = /sdcard/intent_radio

install:
	adb push playlist $(dest)

log:
	adb shell tail -50 /sdcard/Tasker/.intent_radio/log.txt

state:
	adb shell cat /sdcard/Tasker/.intent_radio/state.txt

stop:
	$(MAKE) -C .. stop
