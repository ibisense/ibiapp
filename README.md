ibiapp
======

This repository contains IbiApp Java tool

# Building 

To build the tool you would need JDK (>= 1.7) and Maven build tool (>= 2.0)

```
mvn2 assembly:assembly
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
* Add ability to checkout application whose files does not exist locally but application is available in Ibisense Cloud
* Add packaging for Mac OS, Windows and Linux
* When username or password is wrong, the application must show corresponding error (currently it silently does nothing) 
