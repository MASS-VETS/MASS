@echo off
IF "%ENV%"=="" (
	SET ENV=DEV
)
IF NOT EXIST "%~dp0JARs\" (
	MKDIR "%~dp0JARs\"
)
FOR /F "usebackq tokens=*" %%a IN ("%~dp0build_core_list.txt") DO (
	call:install ..\Code\%%a || goto error
)

FOR /F "usebackq tokens=*" %%a IN ("%~dp0build_list.txt") DO (
	call:compile ..\Code\%%a || goto error
	call:move ..\Code\%%a || goto error
)
goto:eof

:install
call mvn clean install -f "%~dp0%~1\pom.xml" || exit /B 1
goto:eof

:compile
call mvn package -f "%~dp0%~1\pom.xml" || exit /B 1
goto:eof

:move
cd "%~dp0%~1\target"
copy /Y *.jar "%~dp0JARs\"
cd "%~dp0"
goto:eof

:error
cd "%~dp0"
pause
exit /B 1