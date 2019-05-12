# Money transfer

[![CircleCI](https://circleci.com/gh/grigoriy/payments.svg?style=svg&circle-token=9d58a536f8ad651fec9d02d4be0a8d5637d93f71)](https://circleci.com/gh/grigoriy/payments)

Simple and to the point RESTful API (including data model and the backing
implementation) for money transfers between accounts.

### Compile, run tests and build an executable JAR:
```
sbt coverage test coverageReport coverageOff scalafmtAll scalastyle test:scalastyle assembly
```

### Run
```
sbt run
```
or
```
java -jar <projectDir>target/scala-2.12/scala-cli-project-template-assembly-0.1.jar
```

### Notes
* API is described in an OpenAPI spec
* may be invoked by multiple systems and services on behalf of end users
* avoids using heavy frameworks
* data store runs in memory
* executable needs only Java or SBT to run
* functionality is covered with tests