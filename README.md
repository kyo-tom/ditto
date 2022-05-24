# ditto

## What is ditto
A tool for impala sql to trino sql

## Required
- jdk 11+

## Build
```shell
mvn clean package -DskipTests
```

## Usage
1. cli
    ```shell
   cd ditto
   java -jar -Dconfig=ditto-parser/etc/config.properties ditto-parser/target/ditto-parser-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
2. command
   ```shell
   cd ditto
   java -jar -Dconfig=ditto-parser/etc/config.properties ditto-parser/target/ditto-parser-1.0-SNAPSHOT-jar-with-dependencies.jar -f ditto-parser/etc/test.sql
    ```