# Money transfer

[![CircleCI](https://circleci.com/gh/grigoriy/payments.svg?style=svg&circle-token=9d58a536f8ad651fec9d02d4be0a8d5637d93f71)](https://circleci.com/gh/grigoriy/payments)

Simple and to the point RESTful API (including data model and the backing
implementation) for money transfers between accounts.

### API docs
[Documentation](https://grigoriy.github.io/payments-api/swagger.html)

### Compile, run tests and build an executable JAR:
```
sbt test it:test assembly
```

### Run
```
$<project_dir> sbt run
```
or
```
$<project_dir> java -jar target/scala-2.12/payments-assembly-0.1.jar
```

### Notes
* the API is described in an OpenAPI spec (src/main/resources/api_doc/main.yaml)
* may be invoked by multiple systems and services on behalf of end users
* avoids using heavy frameworks
* data store runs in memory
* needs only Java or SBT to run
* functionality is covered with tests
* no unit tests but end-to-end coverage is over 90% (e2e tests were cheaper in development in this case)

### Assumptions (most are limiting but relatively easy to compensate in a production-ready app)
* authorisation, HTTPS, are non-goals
* each request is sent and received exactly once (hence no duplicate requests handling needed)
* the data must be available only while the app is running
* transfers are internal and should be instant, synchronous
* all money is in the same currency
* deposits, withdrawals, account details are non-goals; account creation/retrieval and transfer retrieval are implemented for demonstrating and testing purposes
* strict REST is not required
