@REM ----------------------------------------------------------------------------
@REM SiLoVi bridge start script
@REM ----------------------------------------------------------------------------

@REM set JAVA_HOME=

if not "%JAVA_HOME%"=="" goto OkJHome
goto defaultJava

:OkJHome
set JAVACMD=%JAVA_HOME%\bin\java.exe
goto start

:defaultJava
set JAVACMD=java
goto start

:start
%JAVACMD% -jar .\bin\silovi-bridge.jar --spring.config.location=file:config.properties