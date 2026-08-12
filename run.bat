@echo off
REM ===== Simple run script for testing Guess Market during development =====
REM (This is NOT the final submission batch file. It compiles the source and runs it.)

if not exist engine\out mkdir engine\out
if not exist ui\out mkdir ui\out

echo Compiling engine...
javac -d engine\out engine\src\guessmarket\engine\*.java
if errorlevel 1 goto error

echo Compiling ui...
javac -cp engine\out -d ui\out ui\src\guessmarket\ui\*.java
if errorlevel 1 goto error

echo Starting Guess Market...
echo.
java -cp "engine\out;ui\out" guessmarket.ui.Main
goto end

:error
echo.
echo Compilation failed. Please check the messages above.

:end
echo.
pause
