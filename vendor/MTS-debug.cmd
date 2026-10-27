@echo off
cd /d "%~dp0"
echo Starting ModTheSpire with a console for troubleshooting...
"jre\bin\java.exe" -Xmx1g -jar "ModTheSpire.jar"
echo.
echo ModTheSpire exited. Press any key to close.
pause >nul
