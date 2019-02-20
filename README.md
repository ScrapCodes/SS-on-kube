# Streaming Benchmark.

## Goal

1. A benchmark suite that is easily reproducible and scalable.

2. Completely based on kubernetes for deployment.

3. Models real world scenarios for benchmarking streaming applications.

4. Compares results by writing optimum code for each streaming framework.


## STATUS: WORK IN PROGRESS.

### Usage

This is a sbt project, you can compile code with `sbt compile` and run it
with `sbt run`.

1. Start zookeeper cluster using kubernetes.
`sbt run zk-start`

2. Stop zookeeper cluster, started in previous command.
`sbt run zk-stop`

3. Start Kafka cluster using kubernetes.
__Please note kubernetes cluster needs zookeeper cluster above running.__

`sbt run kafka-start`

4. Stop Kafka cluster, started in previous command.
`sbt run kafka-stop`

### Contributions welcome!

Please see the issues section on what help we need.

Also open issues, if you have good idea on what this benchmark suite should also include.


### Thanks!