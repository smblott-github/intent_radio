
srv = smblott.org
www = public_html/intent_radio/

versioncode = $(shell make -s vercode)

debug: ir_library/res/raw/tasker.prj
	ant debug

release: ir_library/res/raw/tasker.prj
	$(MAKE) clean
	ant release
	mkdir -p releases
	install -v -m 0444 bin/IntentRadio-release.apk releases/IntentRadio-release-general-$(versioncode).apk
	cp bin/IntentRadio-release.apk $(HOME)/storage/Dropbox/Public/IntentRadio-release.apk
	rsync bin/IntentRadio-release.apk $(srv):$(www)

clean:
	cd ./ir_library && ant clean
	cd ./google-play-store && ant clean
	ant clean

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

install-release:
	$(MAKE) release
	adb install -r bin/IntentRadio-release.apk

update-project:
	android update project --name "IntentRadio" --target android-19 --path . --subprojects
	cd ./ir_library/ && $(MAKE) update-project

google:
	cd ./google-play-store && $(MAKE) debug

google-release:
	cd ./google-play-store && $(MAKE) release

logcat:
	adb logcat

log:
	adb logcat -s IntentRadio -s MediaPlayer

ir_library/res/raw/tasker.prj: ./ir_library/misc/Radio.prj.xml
	cd ./ir_library/ && make res/raw/tasker.prj

vercode = $(shell sh ./script/version.sh ir_library/res/values/version.xml)

vercode:
	@echo $(vercode)

version:
	vim ./ir_library/res/values/version.xml

.PHONY: debug release clean install install-release update-project logcat log google google-release version

include ./Makefile.test

# Release process:
#
# - bump
#   ** version code in ./AndroidManifest.xml
#   ** version name in ./res/values/strings.xml
# - update release notes in web/index.ascii
# - git commit/push
# - build release APK
# - git tag -a vX.Y
# - git push origin --tags
# - on GitHub, publish release
#   including upload of release APK
# - add link to release APK to web/index.ascii (at bottom)
# - in web: make install
# - git commit/push
#

