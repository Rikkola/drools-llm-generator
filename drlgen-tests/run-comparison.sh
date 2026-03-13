#!/bin/bash
#
# Drools Rule Generation Comparison Runner
#
# Usage:
#   ./run-comparison.sh                    # Run all models on all scenarios
#   ./run-comparison.sh --models granite-code:8b,qwen2.5-coder:14b
#   ./run-comparison.sh --scenarios adult,discount
#   ./run-comparison.sh --output results.csv
#   ./run-comparison.sh --drl-only         # Only test DRL generation
#   ./run-comparison.sh --yaml-only        # Only test YAML generation
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "Drools Rule Generation Comparison"
echo "========================================"

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

# List available models
echo ""
echo "Available Ollama models:"
curl -s http://localhost:11434/api/tags | grep -o '"name":"[^"]*"' | sed 's/"name":"//g' | sed 's/"//g' | while read model; do
    echo "  - $model"
done
echo ""

# Build the project if needed
echo "Building project..."
cd "$PROJECT_ROOT"
mvn compile -pl drools-drl-generation-tests -q

# Run the comparison
echo ""
echo "Starting comparison run..."
echo ""

mvn -pl drlgen-tests exec:java \
    -Dexec.mainClass="com.github.rikkola.drlgen.generation.runner.ComparisonRunner" \
    -Dexec.args="$*" \
    -q

echo ""
echo "Comparison complete!"
