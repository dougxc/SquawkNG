@echo off

rem MPEG

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats example.mpeg.MPEG

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats -XtraceInstructions -XtraceGCVerbose -XtraceThreshold:6411871 -XtraceURL:file://xx.x example.mpeg.MPEG
rem java -jar build.jar traceviewer -map temp/samples.map -sourcepath j2me/src:vm/src:graphics/src:samples/src xx.x



rem CHESS

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats example.chess.Game

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats -XtraceInstructions -XtraceGCVerbose -XtraceThreshold:13630079 -XtraceURL:file://xx.x  example.chess.Game
rem java -jar build.jar traceviewer -map temp/samples.map -sourcepath j2me/src:vm/src:graphics/src:samples/src xx.x



rem MANYBALLS

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats mojo.Main example.manyballs.ManyBalls

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats -XtraceInstructions -XtraceGC -XtraceThreshold:100000 -XtraceURL:file://xx.x mojo.Main example.manyballs.ManyBalls
rem java -jar build.jar traceviewer -map temp/samples.map -sourcepath j2me/src:vm/src:graphics/src:samples/src xx.x



rem CUBES

rem java -server -jar build.jar squawk -Ximage:temp/samples.image -Xstats mojo.Main example.cubes.Cubes

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats -XtraceInstructions -XtraceGC -XtraceThreshold:1000000000 -XtraceURL:file://xx.x -XXnogc mojo.Main example.cubes.Cubes
rem java -jar build.jar traceviewer -map temp/samples.map -sourcepath j2me/src:vm/src:graphics/src:samples/src xx.x



rem KAWT

java -jar build.jar squawk -Ximage:temp/samples.image -Xstats example.kawtdemo.KawtDemo

rem java -jar build.jar squawk -Ximage:temp/samples.image -Xstats -XtraceInstructions -XtraceGC -XtraceThreshold:460268 -XtraceURL:file://xx.x -XXnogc example.kawtdemo.KawtDemo
rem java -jar build.jar traceviewer -map temp/samples.map -sourcepath j2me/src:vm/src:graphics/src:samples/src xx.x