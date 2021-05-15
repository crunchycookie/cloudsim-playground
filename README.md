<h1 align="center">Welcome to the CloudSim Playground 👋</h1>
<p align="center">
  <a href="https://github.com/crunchycookie/cloudsim-playground/blob/master/LICENSE">
    <img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-yellow.svg" target="_blank" />
  </a>
</p>

## ✨ What is CloudSim?

CloudSim is an extensible framework for modelling, and simulating cloud computing infrastructures. It allows researchers/developers to focus on specific system design issues, without worrying 
about configuring and simulation of a cloud computing infrastructure. More information can be found from it's opensource project(https://github.com/Cloudslab/cloudsim).

## ✨ What is the intention of this project?

Writing custom scenarios on top of the CloudSim framework can be a bit difficult, especially for configuring and running. There are some great blog posts about
configuring the CloudSim framework and running the examples, but they do depend on an IDE like eclipse. 

However, depending on an IDE would not be an ideal choice in the long run for reasons like compatibility issues, dependency management, etc.

This project provides a comprehensive build and execution environment for the CloudSim framework by creating a 
Maven based playground for CloudSim. In this way, the IDE dependency is completely eliminated.

## 🚀 Usage

First, there are some pre-requisites required to build the project.

1. JDK 16 or higher.
2. Apache Maven 3.8.1 or higher(may work with lower versions)
3. cloudsim-<version>.jar file obtained from https://github.com/Cloudslab/cloudsim. For this, download the latest release
of CloudSim from the project and extract the downloaded artifact which is an archive file. Required jar file is in the
   `<extracted-artifact>/jars` location.
   
As mentioned, this project is a pure Maven project. However, the CloudSim framework dependency is not available in 
any of the maven artifact repositories. Therefore, this project requires the path of the downloaded CloudSim framework
JAR file, and then the project will install the JAR file in the local Maven repository during the initializing phase.

To provide the JAR file path to the project, obtain the absolute path of the cloudsim-<version>.jar file, and open 
the pom.xml file in this project root. Under the `<properties>` tag, there is a property called `cloudsim-framework-jar-location`.
Replace it's value with the copied value.

Now the project is ready to be initialized. Execute the following command.

`mvn initialize`

This will install the CloudSim JAR file in the local maven repository similar to a standard Maven dependency.

That concludes the project configurations. Users can implement their custom scenarios in the 
`<project-home>/src/main/java/org/crunchycookie/playground/cloudsim/scenarios` location and execute it with the maven
execution plugin. Let's try to do this with the OOTB provided CloudSim scenario called `CreateDatacenter`.

In the above-mentioned location where scenarios reside, there is a java class file called `CreateDatacenter.java`. 
This simulates a datacenter using the CloudSim framework. Also, note that this class has a static void main method which 
executes the simulation. Therefore, the Maven execution plugin can be used to execute it via the following CLI command. 

`mvn exec:java -Dexec.mainClass="org.crunchycookie.playground.cloudsim.examples.CreateDatacenter"`

A log trace similar to the following should be printed to the standard output, which is the current terminal.

```java
<user> cloudsim-playground % mvn exec:java -Dexec.mainClass="org.crunchycookie.playground.cloudsim.examples.CreateDatacenter"
[INFO] Scanning for projects...
[INFO] 
[INFO] ------< org.crunchycookie.playground.cloudsim:cloudsim-examples >-------
[INFO] Building cloudsim-examples 0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ cloudsim-examples ---
Initialising...
Successfully created the datacenter. Here are some stats.
Number of Hosts: 500. Let's check stats of a host.
Number of cores: 8
Amount of Ram(GB): 64
Amount of Storage(TB): 10
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.284 s
[INFO] Finished at: 2021-05-15T01:52:56+05:30
[INFO] ------------------------------------------------------------------------
```
Please note that the `-Dexec.mainClass=` parameter is equal to the `CreateDatacenter` scenario class. Therefore, any 
custom scenario can be executed in this manner by changing `-Dexec.mainClass=` parameter value to the corresponding class.

## 🤝 Contributing

Contributions, issues and feature requests are welcome.

## Code style

JAVA code style by google is followed.

## Author

👤 **Tharindu Bandara**

- Github: [@tharindu-bandara](https://github.com/tharindu-bandara)

## Show your support

Please ⭐️ this repository if this project helped you!

## 📝 License

Copyright © 2021 [crunchycookie](https://github.com/crunchycookie).
This project is [MIT](https://github.com/crunchycookie/cloudsim-playground/blob/master/LICENSE) licensed.
