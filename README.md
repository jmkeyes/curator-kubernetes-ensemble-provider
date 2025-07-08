Kubernetes Ensemble Provider
============================

A Kubernetes-enabled ensemble provider for Apache Curator.

Getting Started
----------------

Add the dependency:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.jmkeyes</groupId>
        <artifactId>curator-kubernetes-ensemble-provider</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Now register the `KubernetesEnsembleProvider` with Curator:

```java
CuratorFramework client = CuratorFrameworkFactory.builder()
    .ensembleProvider(new KubernetesEnsembleProvider("zookeeper:2812"))
    .retryPolicy(new BoundedExponentialBackoffRetry(10, 30000, 100))
    .ensembleTracker(true)
    .build();
```

Now Curator will track the members of the Zookeeper ensemble dynamically.

Contributing
------------

  1. Clone this repository.
  2. Create your branch: `git checkout -b feature/branch`
  3. Commit your changes: `git commit -am "I am developer."`
  4. Push your changes: `git push origin feature/branch`
  5. Create a PR of your branch against the `main` branch.
