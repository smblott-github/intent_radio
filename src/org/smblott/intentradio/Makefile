
default: debug

# Makefile hack:
#
# The following dependencies are circular; GNU make drops them.  However, they allow
# zsh completion to work, and GNU make uses the rule at the bottom.
#

install: install
play: play
stop: stop
pause: pause
restart: restart

%:
	cd ../../../.. && $(MAKE) $@
