#! /usr/bin/env bash
python compress.py <(grep --text '^0x' "$1" | cut -d ':' --fields=1 | awk '(strtonum("0x0f000000") > strtonum($1) || strtonum($1) > strtonum("0x0fffffff"))') "$2"
bzip2 "$2"
