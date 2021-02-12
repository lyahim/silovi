@REM ----------------------------------------------------------------------------
@REM SiLoVi viewer start script
@REM ----------------------------------------------------------------------------

@REM change PORT if default 80 not fit for requirements
@REM set PORT=1000

if exist bin\silovi_viewer-win.exe (
    bin\silovi_viewer-win.exe
) else (
    if not exist node_modules (
		npm install --production
	)
    npm run node
)