#!/bin/bash
#
# Example: Running DRL Generation Tests with Standalone JAR
#
# This script demonstrates how to use the drlgen-tests JAR to run
# custom scenarios against AI models.
#
# Prerequisites:
#   - Java 17+
#   - Ollama running (ollama serve)
#   - At least one model pulled (e.g., ollama pull qwen3-coder-next)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JAR_FILE="$PROJECT_ROOT/drlgen-tests/target/drlgen-tests-1.0.0-SNAPSHOT.jar"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================"
echo "DRL Generation - Standalone JAR Example"
echo "========================================"
echo

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}JAR file not found. Building...${NC}"
    cd "$PROJECT_ROOT"
    mvn package -pl drlgen-tests -DskipTests -q
    echo -e "${GREEN}Build complete.${NC}"
    echo
fi

# Check if Ollama is running
echo -n "Checking Ollama availability... "
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    echo "Error: Ollama is not running at http://localhost:11434"
    echo "Please start Ollama with: ollama serve"
    exit 1
fi
echo

# Show help
echo -e "${BLUE}=== Example 1: Show Help ===${NC}"
echo "Command: java -jar drlgen-tests.jar --help"
echo
java -jar "$JAR_FILE" --help
echo

# Run with custom scenarios directory
echo -e "${BLUE}=== Example 2: Run Custom Scenarios ===${NC}"
echo "Command: java -jar drlgen-tests.jar --scenarios-dir ./scenarios --models qwen3-coder-next"
echo
echo "Running scenarios from: $SCRIPT_DIR/scenarios"
echo

java -jar "$JAR_FILE" \
    --scenarios-dir "$SCRIPT_DIR/scenarios" \
    --models qwen3-coder-next \
    --output-dir "$SCRIPT_DIR/results"

echo
echo -e "${GREEN}Example complete!${NC}"
echo
echo "Results saved to: $SCRIPT_DIR/results/"
echo "CSV report: comparison-results.csv"
