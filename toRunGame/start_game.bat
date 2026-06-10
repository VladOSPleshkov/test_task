echo off
chcp 65001 > nul
title Sea battle
cd /d %~dp0
java -jar "test task.jar"

pause