#!/usr/bin/env bash
set -e

JAVA21="/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6"
export JAVA_HOME="$JAVA21"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")"
echo "=== Building Command Menu Mod (Fabric 1.21.1) ==="
echo "Java: $(java -version 2>&1 | head -1)"
echo ""
gradle build --no-daemon --info 2>&1 | tail -30
echo ""
echo "=== Done! ==="
echo "Install the JAR from:  build/libs/command-menu-1.0.0.jar"
echo "(copy it to your .minecraft/mods/ folder)"
