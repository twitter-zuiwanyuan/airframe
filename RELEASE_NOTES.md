# Release Notes

## 0.10
 - Session now implements AutoClosable
 - Add Design.withSession{ session => ... } to close Session at ease 

## 0.9
 - Add Scala 2.12 support

## 0.8
 - Split the project into two sub projects: airframe and airframe-macro. However, you only need airframe.jar for using Airframe.
 - Improved the performance by replacing run-time code generation to compile-time code generation #25
 - Upgrade to wvlet-log 1.0 
 - Upgrade wvlet-obj 0.26 (support type aliases)
 - Allow binding to type aliases

## 0.7
 - Fix lifecycle management of objects generated by bindSingleton

## 0.6
 - Add bindSingleton[X]

## 0.5 
 - Fix a bug in FILO order init/shutdown when objects are singleton
 - Add toProvider/toSingletonProvider/toEagerSingletonProvider
 - Add Design.remove[X]

## 0.4 
 - Improved binding performance
 - Fix FIFO lifecycle hook executor
 - Improved injection logging

## 0.3 
 - Support bind(ObjectType).toXXX 

## 0.2
 - Add lifecycle manageer
 - Reorganize Session, Design classes
 - Test coverage improvement
 - Depreacted Design.build[X]. Use Design.newSession.build[X]

## 0.1
 - Migrated from wvlet-inject
