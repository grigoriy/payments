# Money transfer

Simple and to the point RESTful API (including data model and the backing
implementation) for money transfers between accounts.

### Notes about the project:
* API is described in an OpenAPI spec
* may be invoked by multiple systems and services on behalf of end users
* avoids using heavy frameworks
* data store runs in memory
* distributive depends only on Java for execution
* the functionality is covered with tests

### Compile, run tests and build an executable JAR:
```
sbt coverage test coverageReport coverageOff scalafmtAll scalastyle test:scalastyle assembly
```

### Run
```
sbt "run <args>"
```
or
```
java -jar <projectDir>target/scala-2.12/scala-cli-project-template-assembly-0.1.jar <args>
```
