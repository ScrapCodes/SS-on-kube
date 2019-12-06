# Deploy an end to end ML pipeline using Apache spark streaming and kubernetes.


## Goal

1. A one click deployment, using kubernetes, Apache kafka and Apache Spark structured streaming.

2. Online evaluation of text sentiment using a pre build model for Text sentiment analysis.

3. For model serving there are many tools available, since the whole 
   pipeline is running inside kubernetes, we will use IBM MAX(Model Asset Exchange).

4. There is also a plan to build a nice UI that displays running stats. (WIP)

## Project Structure

1. Project is divided into 3 sub modules. i.e.
    
    a. **demo** - an end to end ml pipeline, using Apache Spark structured streaming,
     kubernetes, kafka and IBM MAX.
    
    b. **deploy** - a set of kubernetes utilities to deploy the spark, kafka and zookeeper
        clusters. One of the goals of this module is to make writing an end to end
         pipelines as easy as providing some configuration and say start. More on this in 
         upcoming section, how to write cool demos.
    
    c. **integration-tests** - Integration tests, for validating our deployment and benchmark
        automation.
    
2. There is also a plan to add a UI module, but that depends on our bandwidth.

## STATUS: READY TO TRY.

Currently, we have got automated deployment for kafka, zookeeper and spark clusters.
 We also have an end to end demo, which is ready to try, residing in `demo/` directory.
 See the instructions below, in [usage](#usage) section to run the demo. 

### Usage

This is a sbt project, you can install sbt from [here](https://www.scala-sbt.org/download.html).

#### Configure the service account

This step is applicable for both for running the tests or demo.

For working with just the defaults, which is the easiest way to try it out. You can use the 
`default` namespace on kubernetes and create a service account `spark` as follows.

```shell script
kubectl create serviceaccount spark
kubectl create clusterrolebinding spark-role --clusterrole=edit \
     --serviceaccount=default:spark --namespace=default
```

#### Tests
Tests include most of the code examples and usages.

To run the tests, first you need to configure them, provide a service account with correct
 RBAC permission and a namespace.
Please take a look at the individual tests and the `TestBase` and `SparkSuiteBase` classes.

Run them by `sbt test`
#### Running Demo

1. Prepare kubernetes cluster. 

##### Alternative 1.
Run on minikube or single node k8s cluster.

        Start minikube with kubernetes version as 1.14.6
        
        ```shell script
         minikube start --kubernetes-version=1.14.6
        ```
        
##### Alternative 2.
 Run on a real cluster with atleast 3 nodes. (supported k8s version: 1.14.6)
We need a kubernetes cluster with sufficient available resources to run the demo. Minimally
 it should have 3 nodes. The cluster should have minimally 14 cores available and 20 GiB of memory free.
 The reason is:
 
    a. Zookeeper service also run with 3 pods, each with resource requirement of 0.5 cpu and 1GiB
      memory per node. Each pod is configured with anti-affinity, so that each pod runs on a separate
      node. Hence the requirement for 3 node cluster arises. This is done, to accomplish resilience. 

    b. Kafka service runs 3 pods, each with resource requirement of 1 cpu and 2GiB memory per node.

    c. IBM MAX, service runs 2 instances of itself, and has minimum requirement of 1GiB memory and
      1.5 cpu per node.

    d. Apache spark (as of version 3.0.0-preview) by default uses 1.5 GiB of memory and 1 cpu by default for 
      both driver and executor, unless configured to be something else.

Estimating the requirements, we would need a kubernetes cluster with 3 nodes and 14 free
 cpu cores and 20 GiB memory. That is like enormous amount of resources for a demo !, but 
 the purpose of the demo is to demonstrate the use of these components together and since
 each of these components are designed with intention to scale to a large number of nodes.
 This initial demo can be converted into a large scale end to end ML pipeline, with just 
 some configuration changes. For example, simply changing the replica counts of different
 services and increasing the executor count in spark. See alternative 1, for running with
 minimum resources, locally e.g. laptop.
 
2. Setup service accounts:
Please follow the instruction from the section 
[Configure the service account](#configure-the-service-account).

3. Clone this repository.
```shell script
git clone https://github.com/ScrapCodes/SS-on-kube.git
cd SS-on-kube/
```
4. Run the demo.
```shell script
sbt "demo/runMain org.codait.sb.demo.deploy.KubeDeployDemo"
```

### Thanks!

We would love to have an early feedback. Please help us by opening issues.
