
srv = smblott.org
www = public_html/intent_radio/

debug: ir_library/res/raw/tasker.prj
	ant debug

release: ir_library/res/raw/tasker.prj
	$(MAKE) clean
	ant release
	cp bin/IntentRadio-release.apk $(HOME)/storage/Dropbox/Public/IntentRadio-release.apk
	rsync bin/IntentRadio-release.apk $(srv):$(www)

clean:
	ant clean
	cd ./ir_library && ant clean
	cd ./google-play-store && ant clean

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

install-release:
	$(MAKE) release
	adb install -r bin/IntentRadio-release.apk

update-project:
	android update project --name "IntentRadio" --target android-19 --path . --subprojects
	cd ./ir_library/ && $(MAKE) update-project

logcat:
	adb logcat -s IntentRadio -s MediaPlayer

log:
	$(MAKE) logcat

ir_library/res/raw/tasker.prj: ./ir_library/misc/Radio.prj.xml
	cd ./ir_library/ && make $@

.PHONY: debug release clean install install-release update-project logcat log

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

