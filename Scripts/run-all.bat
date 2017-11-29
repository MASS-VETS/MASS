@echo off
IF "%ENV%"=="" (
	SET ENV=DEV
)
echo starting all microservices
cd "%~dp0JARs\"
FOR /F "usebackq tokens=*" %%a IN ("%~dp0start_config.txt") DO (
		call:start %%a
)
cd "%~dp0"
echo started.
goto:eof

:start
SET PRJ=%~1
if "%PRJ:~0,1%" NEQ "#" (
	echo Starting %*
	start "%PRJ%" /d "%~dp0JARs\" java -jar %*
)
goto:eof