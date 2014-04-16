
debug: ir_library ir_library/res/raw/tasker.prj
	ant debug

srv = smblott.org
www = public_html/intent_radio/

release: ir_library ir_library/res/raw/tasker.prj
	$(MAKE) clean
	ant release

clean:
	cd .. && $(MAKE) clean

ir_library:
	ln -s ../ir_library .

install: ir_library
	$(MAKE) debug
	adb install -r bin/IntentRadio-debug.apk

install-release:
	$(MAKE) release
	adb install -r bin/IntentRadio-release.apk

logcat:
	cd .. && $(MAKE) $@

log:
	cd .. && $(MAKE) $@

update-project: ir_library
	android update project --name "IntentRadio" --target android-19 --path . --subprojects
	cd ./ir_library/ && $(MAKE) update-project

ir_library/res/raw/tasker.prj: ./ir_library ./ir_library/misc/Radio.prj.xml
	cd ./ir_library/ && $(MAKE) $@

.PHONY: debug release clean install install-release logcat log update-project
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

