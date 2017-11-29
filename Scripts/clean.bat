@echo off
echo cleaning...
cd "%~dp0JARs\"
del *.jar
del id_file
rmdir /s /q logs
FOR /D %%a IN (..\Code\*) DO (
	call:clean %%a
)
cd "%~dp0"
echo done.
goto:eof

:clean
echo %~1
cd "%~dp0%~1\target"
del *.jar
del *.jar.original
goto:eof