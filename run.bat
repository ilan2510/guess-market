@echo off
REM ===== Simple run script for testing Guess Market during development (Part 2) =====
REM (This is NOT the final submission batch file. It compiles the source and runs it.)
REM Needs the JavaFX SDK. Change JAVAFX_LIB below if it's installed somewhere else.

set JAVAFX_LIB=C:\Users\ilan2\javafx-sdk-25\lib

if not exist engine\out mkdir engine\out
if not exist ui\out mkdir ui\out

echo Compiling engine...
if exist engine_sources.txt del engine_sources.txt
for /f "delims=" %%f in ('dir /s /b engine\src\*.java') do (
    set "srcpath=%%f"
    setlocal enabledelayedexpansion
    echo "!srcpath:\=/!" >> engine_sources.txt
    endlocal
)
javac -d engine\out @engine_sources.txt
del engine_sources.txt
if errorlevel 1 goto error

echo Compiling ui...
if exist ui_sources.txt del ui_sources.txt
for /f "delims=" %%f in ('dir /s /b ui\src\*.java') do (
    set "srcpath=%%f"
    setlocal enabledelayedexpansion
    echo "!srcpath:\=/!" >> ui_sources.txt
    endlocal
)
javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp engine\out -d ui\out @ui_sources.txt
del ui_sources.txt
if errorlevel 1 goto error

echo Starting Guess Market...
echo.
java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp "engine\out;ui\out" guessmarket.ui.Main
goto end

:error
echo.
echo Compilation failed. Please check the messages above.

:end
echo.
pause
