# Deploy an end to end ML pipeline using Apache spark streaming and kubernetes.


## Goal

1. A one click deployment, using kubernetes, Apache kafka and Apache Spark structured streaming.

2. Online evaluation of text sentiment using a pre build model for Text sentiment analysis.

3. For model serving there are many tools available, since the whole 
   pipeline is running inside kubernetes, we will use IBM MAX(Model Asset Exchange).

4. There is also a plan to build a nice UI that displays running stats.

## Project Structure

1. Project is divided into 3 sub modules. i.e.
    
    a. **demo** - a set of benchmark helpers. This is where we will keep our spark streaming
        job for object detection from incoming images.
    
    b. **deploy** - a set of kubernetes utilities to deploy the spark, kafka and zookeeper
        clusters.
    
    c. **integration-tests** - Integration tests, for validating our deployment and benchmark
        automation.
    
2. There is also a plan to add a UI module, but that depends on our bandwidth.

## STATUS: WORK IN PROGRESS.

Currently, we have got automated deployment for kafka, zookeeper and spark clusters.
As of now, spark cluster supports running only the cluster mode for deployment.
Next we would like to run an end to end streaming pipeline with standard kafka example. 

### Usage

This is a sbt project, you can compile code with `sbt compile` and run it
with `sbt run`.

Tests include most of the code examples and usages.

To run the tests, first you need to configure them. Please take a look at the individual tests.
Run them by `sbt test`

### Thanks!

We would love to have an early feedback. Please help us by opening issues.
