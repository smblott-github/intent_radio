
include $(HOME)/.gnumake.mk

.PHONY: build install

target += Radio.prj.xml 
target += index.html
target += IntentRadio-release.apk

build: $(target)
	@true

srv = smblott.org
www = /home/www/smblott.org/intent_radio

install:
	$(MAKE) build
	rsync -av $(target) $(srv):$(www)

Radio.prj.xml: ../misc/Radio.prj.xml
	install -m 0444 $< $@

IntentRadio-release.apk: ../bin/IntentRadio-release.apk
	install -m 0444 $< $@
