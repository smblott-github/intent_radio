
dest = /sdcard/intent_radio

install:
	$(MAKE) -C ../web
	adb push playlist $(dest)

log:
	adb shell tail -50 /sdcard/Tasker/.intent_radio/log.txt

state:
	adb shell cat /sdcard/Tasker/.intent_radio/state.txt

