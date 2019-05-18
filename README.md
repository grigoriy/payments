# Money transfer

[![CircleCI](https://circleci.com/gh/grigoriy/payments.svg?style=svg&circle-token=9d58a536f8ad651fec9d02d4be0a8d5637d93f71)](https://circleci.com/gh/grigoriy/payments)

Simple and to the point RESTful API (including data model and the backing
implementation) for money transfers between accounts.

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
* API is described in an OpenAPI spec
* may be invoked by multiple systems and services on behalf of end users
* avoids using heavy frameworks
* data store runs in memory
* needs only Java or SBT to run
* functionality is covered with tests
* no unit tests but end-to-end coverage is over 90% (e2e tests were cheaper in development in this case)

### Assumptions
* all money is in the same currency
* each request comes exactly once (hence no duplicate requests handling)
* no deposits, withdrawals, account details, transfer details are needed
* the data must be available only while the app is running
* strict REST is not required
