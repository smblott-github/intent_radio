
include $(HOME)/.gnumake.mk

.PHONY: build install

target += Radio.prj.xml 
target += index.html
target += debug.html
target += tasker.html

build: $(target)
	@true

srv = godaddy
www = public_html/intent_radio/

install:
	$(MAKE) build
	rsync -av --delete $(target) $(srv):$(www)

Radio.prj.xml: ../misc/Radio.prj.xml
	install -m 0444 $< $@

