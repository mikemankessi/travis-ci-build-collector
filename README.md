Build Collector
=================

This project uses Spring Boot to package the collector as an executable JAR with dependencies.

Building and Deploying
--------------------------------------

Run
```
mvn install
```
to package the collector into an executable JAR file. Copy this file to your server and launch it using :
```
java -jar travis-ci-build-collector-2.0.0-SNAPSHOT.jar --travis-ci.cron="0 0/5 * * * *"
```
You can override with a properties file **application.properties** file that contains information about how
to connect to the Dashboard MongoDB database instance, as well as properties the Hudson collector requires. See
the Spring Boot [documentation](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-external-config-application-property-files)
for information about sourcing this properties file.

###Sample application.properties file
--------------------------------------

    #Database Name 
    dbname=dashboard

    #Database HostName - default is localhost
    dbhost=localhost

    #Database Port - default is 27017
    dbport=27017

    #Database Username - default is blank
    dbusername=db

    #Database Password - default is blank
    dbpassword=dbpass

    #Collector schedule (required)
    travis-ci.cron=0 0/5 * * * *



