
dest = /sdcard/intent_radio

install: numbers.txt
	adb push playlist $(dest)
	adb push numbers.txt $(dest)

numbers.txt:
	seq --format="%03.0f" 0 1 999 >$@

