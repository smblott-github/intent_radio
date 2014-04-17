
versioncode = $(shell sh ../script/version.sh)

debug: ../ir_library/res/raw/tasker.prj
	ant debug

release: ../ir_library/res/raw/tasker.prj
	$(MAKE) clean
	ant release
	mkdir -p ../releases
	install -v -m 0444 bin/IntentRadio-release.apk ../releases/IntentRadio-release-google-$(versioncode).apk

install:
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

install-release:
	$(MAKE) release
	adb install -r bin/IntentRadio-release.apk

clean:
	cd .. && $(MAKE) clean

logcat:
	cd .. && $(MAKE) $@

log:
	cd .. && $(MAKE) $@

update-project:
	android update project --name "IntentRadio" --target android-19 --path . --subprojects
	cd ../ir_library/ && $(MAKE) update-project

../ir_library/res/raw/tasker.prj: ../ir_library/misc/Radio.prj.xml
	cd ../ir_library/ && $(MAKE) res/raw/tasker.prj

version:
	@echo $(versioncode)

.PHONY: debug release clean install install-release logcat log update-project versioncode
include ../Makefile.test

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

