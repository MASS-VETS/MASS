@echo off
FOR /D %%a IN (..\Code\*) DO (
	call:compile %%a || goto error
)
goto:eof

:compile
echo compiling %~1...
cd %~dp0%~1
call mvn package || exit /B 1
cd %~dp0%~1\target
copy /Y *.jar %~dp0JARs\
cd %~dp0
goto:eof

:error
cd %~dp0
pause
exit /B 1