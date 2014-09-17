
include $(HOME)/.gnumake.mk

.PHONY: build install

target += Radio.prj.xml 
target += IR_Playlist.prj.xml
target += index.html
target += playlist.html
target += debug.html
target += tasker.html
target += privacy.html
target += release.html
target += favicon.ico
target += playlist

build: $(target)
	@true

srv = smblott.org
www = public_html/intent_radio/

install:
	$(MAKE) build
	chmod ugo+r *
	rsync -av $(target) $(srv):$(www)

Radio.prj.xml: ../ir_library/misc/Radio.prj.xml
	install -m 0444 $< $@

playlist: ../tasker_scripts/playlist
	install -m 0444 $< $@

