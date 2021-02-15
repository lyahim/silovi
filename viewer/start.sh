#!/bin/sh
# ----------------------------------------------------------------------------
# SiLoVi viewer start script
# ----------------------------------------------------------------------------

# change PORT if default 80 not fit for requirements
# export PORT=1000

./bin/silovi_viewer-linux > ./logs/silovi.log 2>&1 &