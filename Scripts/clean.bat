@echo off
echo cleaning...
cd "%~dp0JARs\"
del *.jar
del id_file
rmdir /s /q logs
cd "%~dp0"
echo cleaning ..\Code\*
FOR /D %%a IN (..\Code\*) DO (
	echo %%a
	rmdir /S /Q "%~dp0%%a\target"
)
cd "%~dp0"
echo done.
goto:eof

:clean
echo %~1
rmdir /S /Q "%~dp0%~1\target"
goto:eof