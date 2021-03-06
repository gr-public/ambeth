@echo off

SETLOCAL
IF NOT DEFINED BUNDLE_HOME ( call "..\..\..\..\setenv.bat" )

rem Define all needed variables
set baseDir=%~dp0\target
set srcDir=%baseDir%\src

set srcHome=%BUNDLE_HOME%\source
set javaSrcDir=%srcDir%\java
set javaLibDir=%javaSrcDir%\libs
set javaModuleDir=%srcHome%\ambeth\jambeth
set javaModuleDir2=%srcHome%\ambeth
set integrityDir=%srcHome%\ambeth-integrity

set csSrcDir=%srcDir%\cs
set csLibDir=%csSrcDir%\libs
set csModuleDir=%srcHome%\ambeth\ambeth
set csAmbethProperties=%csSrcDir%\ambeth.properties
set csSkipModuleScan=false

set resultType=tc
set dataDir=%baseDir%\data


rem If desired removed the target dir to start clean
rmdir /s /q %baseDir% 1> nul 2> nul

rem Create all needed folders if neccesary
mkdir %baseDir% 1> nul 2> nul
mkdir %srcDir% 1> nul 2> nul

mkdir %javaSrcDir% 1> nul 2> nul
mkdir %javaLibDir% 1> nul 2> nul

mkdir %csSrcDir% 1> nul 2> nul
mkdir %csLibDir% 1> nul 2> nul

mkdir %dataDir% 1> nul 2> nul

rem Copy all jAmbeth jars
rem call mvn -f %javaModuleDir2%\pom.xml dependency:copy-dependencies -DoutputDirectory=%javaSrcDir% -DincludeGroupIds=com.koch.ambeth

rem Copy all external library jars
rem call mvn -f %javaModuleDir2%\pom.xml dependency:copy-dependencies -DoutputDirectory=%javaLibDir% -DexcludeGroupIds=com.koch.ambeth

rem Copy all jAmbeth jars
call mvn -f %javaModuleDir%\pom.xml dependency:copy-dependencies -DoutputDirectory=%javaSrcDir% -DincludeGroupIds=com.koch.ambeth

rem Copy all external library jars
call mvn -f %javaModuleDir%\pom.xml dependency:copy-dependencies -DoutputDirectory=%javaLibDir% -DexcludeGroupIds=com.koch.ambeth

rem Copy all C# libs
for /r %csModuleDir% %%x in (Ambeth.*.dll Ambeth.*.pdb Minerva.*.dll Minerva.*.pdb) do (
  copy "%%x" "%csSrcDir%\" > nul
)

del %csSrcDir%\*.SL*.dll
del %csSrcDir%\*.SL*.pdb

rem Copy all external library libs
xcopy "%csModuleDir%\Ambeth.Util\libs\*.dll" "%csLibDir%" /I /Y > nul
xcopy "%csModuleDir%\Lib.Telerik\*.dll" "%csLibDir%" /I /Y > nul

rem From Jenkins Job
set resultType=tcs
set javaModules=jambeth-audit-server,jambeth-bytecode,jambeth-cache,jambeth-cache-bytecode,jambeth-cache-datachange,jambeth-cache-server,jambeth-cache-stream,jambeth-datachange,jambeth-datachange-kafka,jambeth-datachange-persistence,jambeth-event,jambeth-event-datachange,jambeth-event-kafka,jambeth-event-server,jambeth-filter,jambeth-ioc,jambeth-job,jambeth-job-cron4j,jambeth-log,jambeth-mapping,jambeth-merge,jambeth-merge-bytecode,jambeth-merge-server,jambeth-persistence,jambeth-persistence-api,jambeth-persistence-h2,jambeth-persistence-jdbc,jambeth-persistence-oracle11,jambeth-platform,jambeth-query,jambeth-query-inmemory,jambeth-query-jdbc,jambeth-rdf,jambeth-security,jambeth-security-bytecode,jambeth-security-server,jambeth-sensor,jambeth-server-rest,jambeth-service,jambeth-stream,jambeth-testutil,jambeth-testutil-persistence,jambeth-util,jambeth-xml
set csModules=Ambeth.Bytecode,Ambeth.Cache,Ambeth.Cache.Bytecode,Ambeth.CacheDataChange,Ambeth.DataChange,Ambeth.Event,Ambeth.Event.DataChange,Ambeth.Filter,Ambeth.IoC,Ambeth.Log,Ambeth.Mapping,Ambeth.Merge,Ambeth.Merge.Bytecode,Ambeth.Privilege,Ambeth.Security,Ambeth.Service,Ambeth.TestUtil,Ambeth.Util,Ambeth.Xml

echo -DjarFolders="%javaSrcDir%" -DlibraryJarFolders="%javaLibDir%" -DtargetPath="%dataDir%" -DmoduleRootPath="%javaModuleDir%"
@call mvn exec:java -Dexec.mainClass="com.koch.classbrowser.java.Program" -DjarFolders="%javaSrcDir%" -DlibraryJarFolders="%javaLibDir%" -DtargetPath="%dataDir%" -DmoduleRootPath="%javaModuleDir%"

rem Create xml containing the description of the C# code
set csClassBrowserDir=%integrityDir%\com.koch.classbrowser.csharp\CsharpClassbrowser

rem @C:\Windows\Microsoft.NET\Framework64\v4.0.30319\msbuild.exe "%csClassBrowserDir%\CsharpClassbrowser.sln" "/p:ContinueOnError=false" "/p:StopOnFirstFailure=true"
rem @"%csClassBrowserDir%\CsharpClassbrowser\bin\Debug\CsharpClassbrowser.exe" -assemblyPaths="%csSrcDir%" -libraryAssemblyPaths="%csLibDir%" -targetPath="%dataDir%" -moduleRootPath="%csModuleDir%"

copy "%dataDir%\export_java.xml" "%dataDir%\export_csharp.xml" /Y

:end
pause