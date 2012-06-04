#!/bin/sh

if test -f ~/.sbtconfig; then
  . ~/.sbtconfig
fi

java -Dfile.encoding=UTF8 -XX:MaxPermSize=256m -Xmx1024M -Xss2M -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -jar `dirname $0`/sbt-launch-0.11.3.jar "$@"
