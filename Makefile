
srv = smblott.org
www = public_html/intent_radio/

versioncode = $(shell sh ./script/version.sh)

debug: ir_library/res/raw/tasker.prj
	ant debug

release: ir_library/res/raw/tasker.prj
	$(MAKE) clean
	ant release
	mkdir -p releases
	install -v -m 0444 bin/IntentRadio-release.apk releases/IntentRadio-release-general-$(versioncode).apk
	rsync -v releases/*.apk $(HOME)/storage/Dropbox/Public/
	rsync -v bin/IntentRadio-release.apk $(srv):$(www)

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

version:
	@echo $(versioncode)

.PHONY: debug release clean install install-release update-project logcat log google google-release version

include ./Makefile.test

# Release process:
#
# - bump:
#      - version code and name in BOTH ./AndroidManifest.xml and ./google-play-store/AndroidManifest.xml
# - update release notes in web/index.ascii:
#      make install
# - git commit; git push
# - gtag vX.Y.Z
# - build release APKs:
#      make release
#      make google-release
# - F-Droid:
#      - update metadata
#      - submit PR
# - Google Play Store:
#      - upload new APK
#

