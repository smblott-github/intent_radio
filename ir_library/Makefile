
debug: res/raw/tasker.prj
	cd .. && $(MAKE) $@

res/raw/tasker.prj:
	install -m 0444 $< $@

clean:
	cd .. && $(MAKE) $@

install:
	cd .. && $(MAKE) $@

install-release:
	cd .. && $(MAKE) $@

logcat:
	cd .. && $(MAKE) $@

log:
	$(MAKE) logcat

update-project:
	android update lib-project --target android-19 --path .

.PHONY: debug clean install install-release logcat log update-project
include ../Makefile.test

