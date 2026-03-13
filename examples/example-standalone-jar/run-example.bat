@echo off
REM
REM Example: Running DRL Generation Tests with Standalone JAR
REM
REM This script demonstrates how to use the drlgen-tests JAR to run
REM custom scenarios against AI models.
REM
REM Prerequisites:
REM   - Java 17+
REM   - Ollama running (ollama serve)
REM   - At least one model pulled (e.g., ollama pull qwen3-coder-next)
REM

setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%..\..\"
set "JAR_FILE=%PROJECT_ROOT%drlgen-tests\target\drlgen-tests-1.0.0-SNAPSHOT.jar"

echo ========================================
echo DRL Generation - Standalone JAR Example
echo ========================================
echo.

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo JAR file not found. Building...
    cd "%PROJECT_ROOT%"
    call mvn package -pl drlgen-tests -DskipTests -q
    if errorlevel 1 (
        echo ERROR: Build failed
        exit /b 1
    )
    echo Build complete.
    echo.
)

REM Check if Ollama is running
echo Checking Ollama availability...
curl -s http://localhost:11434/api/tags >nul 2>&1
if errorlevel 1 (
    echo ERROR: Ollama is not running at http://localhost:11434
    echo Please start Ollama with: ollama serve
    exit /b 1
)
echo Ollama is available.
echo.

REM Show help
echo === Example 1: Show Help ===
echo Command: java -jar drlgen-tests.jar --help
echo.
java -jar "%JAR_FILE%" --help
echo.

REM Run with custom scenarios directory
echo === Example 2: Run Custom Scenarios ===
echo Command: java -jar drlgen-tests.jar --scenarios-dir .\scenarios --models qwen3-coder-next
echo.
echo Running scenarios from: %SCRIPT_DIR%scenarios
echo.

java -jar "%JAR_FILE%" ^
    --scenarios-dir "%SCRIPT_DIR%scenarios" ^
    --models qwen3-coder-next ^
    --output-dir "%SCRIPT_DIR%results"

echo.
echo Example complete!
echo.
echo Results saved to: %SCRIPT_DIR%results\
echo CSV report: comparison-results.csv
