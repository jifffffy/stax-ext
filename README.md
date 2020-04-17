STAX Ext repository 
=========

This is my STAX ext repo for the Software Testing Automation Framework (STAF) 
 [http://staf.sourceforge.net/]. The Software Testing Automation Framework (STAF) is an open source, 
 multi-platform, multi-language framework designed around the idea of reusable components, 
 called services (such as process invocation, resource management, logging, and monitoring). 
 STAF removes the tedium of building an automation infrastructure, thus enabling you to focus on building your automation solution. 
 The STAF framework provides the foundation upon which to build higher level solutions, and provides a pluggable approach supported across a large variety of platforms and languages..

Features:
---------
- 1、 upgrade jython2.5 to jython2.7;
- 2、 support function STAX event;

Build:
---------
Run the below command:
```
cd stax
gradle clean build
gradle staf
```
The command above generates one jar: ``` STAX.jar```, then copy the jar to ```(your STAF install dir)/services/stax/``` dir,
and update the STAF.cfg:
```
SERVICE STAX LIBRARY JSTAF EXECUTE {STAF/Config/STAFRoot}/services/stax/STAX.jar
```
TODO:
-----
- 1、 add dialog element;
- 2、 create a new monitor;
