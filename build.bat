@REM ----------------------------------------------------------------------------
@REM SiLoVi builder script
@REM ----------------------------------------------------------------------------

@REM set JAVA_HOME=

rd /s /q build

cd bridge

cmd /C mvn clean install package -DskipTests

cd ../viewer

cmd /C npm run build && npm run copy & npm run pkg && npm run zip-bundle && rd /s /q ..\build\silovi-viewer && npm run build && npm run copy & npm run copy-dist && npm run zip

cd ..