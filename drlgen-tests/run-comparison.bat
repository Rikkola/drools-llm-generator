@echo off
REM
REM Drools Rule Generation Comparison Runner
REM
REM Usage:
REM   run-comparison.bat                             Run all models on all scenarios
REM   run-comparison.bat --models qwen3-coder-next   Run specific model
REM   run-comparison.bat --scenarios adult,discount  Filter scenarios
REM   run-comparison.bat --output results.csv        Custom output file
REM   run-comparison.bat --help                      Show help
REM

setlocal EnableDelayedExpansion

echo ========================================
echo Drools Rule Generation Comparison
echo ========================================

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

REM Get script directory
set "SCRIPT_DIR=%~dp0"

REM Check if JAR exists
set "JAR_FILE=%SCRIPT_DIR%target\drlgen-tests-1.0.0-SNAPSHOT.jar"
if not exist "%JAR_FILE%" (
    echo JAR file not found. Building project...
    cd "%SCRIPT_DIR%.."
    call mvn package -pl drlgen-tests -DskipTests -q
    if errorlevel 1 (
        echo ERROR: Build failed
        exit /b 1
    )
    echo Build complete.
    echo.
)

REM Run the comparison
echo Starting comparison run...
echo.
java -jar "%JAR_FILE%" %*

echo.
echo Comparison complete!
