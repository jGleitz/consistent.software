#!/bin/sh
case "$0" in
  *explicit-chrome*) label=explicit; version="$TEST_EXPLICIT_CHROME_VERSION" ;;
  *) label=default; version="$TEST_DEFAULT_CHROME_VERSION" ;;
esac

# Keep the file unchanged so the tests isolate re-evaluation of the version probe.
if [ "$1" = "--version" ]; then
  printf '%s\n' "$version"
else
  printf '%s\n' "$label" >> "$TEST_CHROME_LAUNCH_LOG"
  exec "$TEST_CHROME_EXECUTABLE" "$@"
fi
