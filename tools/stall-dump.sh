#!/bin/bash
# Takes a thread dump of the running Minecraft dev client every 10 seconds for 3 minutes, into build/stall/.
# Start this first, then start ./gradlew runClient in another Terminal window and load your world.
cd "$(dirname "$0")/.."
mkdir -p build/stall
JSTACK="$(/usr/libexec/java_home -v 21 2>/dev/null)/bin/jstack"
[ -x "$JSTACK" ] || JSTACK="$(command -v jstack)"
if [ -z "$JSTACK" ]; then echo "jstack not found: install a Java 21 JDK"; exit 1; fi
echo "Waiting for the game to start..."
for i in $(seq 1 18); do
  PID=$(pgrep -f 'net.neoforged.*(DevLaunch|devlaunch|BootstrapLauncher)|cpw.mods.bootstraplauncher|neoforge.*Main' | head -1)
  if [ -n "$PID" ]; then
    "$JSTACK" "$PID" > "build/stall/dump-$(date +%H%M%S).txt" 2>&1 && echo "dump $i taken"
  fi
  sleep 10
done
echo "Done: dumps are in build/stall/"
