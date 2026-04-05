JAVA		= java --enable-preview
PHASE		=

.PHONY  : %
%       : %.pins25
	$(JAVA) -classpath ../bin pins25.phase.$(PHASE) $<

