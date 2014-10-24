ibiapp
======

This repository contains IbiApp Java tool

# Building 

To build the tool you would need JDK (>= 1.7) and Maven build tool (>= 3.0)

```
mvn3 package
```

# Usage

Help information:
```
java -cp <path to ibiapp.jar>/ibiapp.jar com.ibisense.ibiapp.Main -h
```

Initialize new ibisense app:
```
java -cp <path to ibiapp.jar>/ibiapp.jar com.ibisense.ibiapp.Main init <application name>
```

Deploy application:
```
java -cp <path to ibiapp.jar>/ibiapp.jar com.ibisense.ibiapp.Main deploy <application name>
```

Delete application:

```
java -cp <path to ibiapp.jar>/ibiapp.jar com.ibisense.ibiapp.Main remove <application name>
```

# TODO
* Add ability to checkout application whose files do not exist locally, but an application is available in Ibisense Cloud
* Add packaging for Mac OS, Windows and Linux
* When username and/or password are/is wrong, the application must show meaningful error (currently it silently does nothing) 
