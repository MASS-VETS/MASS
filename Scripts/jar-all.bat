@echo off
call:install ..\Code\MicroserviceCore
call:move ..\Code\MicroserviceCore
FOR /D %%a IN (..\Code\*) DO (
	if not "%%a"=="..\Code\MicroserviceCore" (
		call:compile %%a || goto error
		call:move %%a || goto error
	)
)
goto:eof

:install
call mvn clean install -f %~dp0%~1\pom.xml || exit /B 1
goto:eof

:compile
call mvn package -f %~dp0%~1\pom.xml || exit /B 1
goto:eof

:move
cd %~dp0%~1\target
copy /Y *.jar %~dp0JARs\
cd %~dp0
goto:eof

:error
cd %~dp0
pause
exit /B 1