# Overview
This repository contains a WildFly Swarm service intended to test the performance of storage with frequent writes of small files. It does so creating a new Git repo, copying 94 files into it and committing them to git, (optionally) one by one.

# Build and Deployment
First, clone this repository:

````
$ git clone git@github.com:bmozaffa/storage-performance-test.git storage-performance-test
````

Change directory to the root of this project:

````
$ cd storage-performance-test
````

Build and deploy using Maven:

````
$ mvn clean fabric8:deploy -Popenshift
````

# Pre-built image
An image is pre-built for convenience and can be pulled anonymously as follows:

````
$ docker pull docker-registry.engineering.redhat.com/bmozaffa/storage-performance
````

# Running a test
To create a new OpenShift project and run this test using ephemeral storage that is local to the container:

````
$ oc new-project storage-performance
$ oc new-app --docker-image=docker-registry.engineering.redhat.com/bmozaffa/storage-performance --name=performance-test
````

To run the test where files added to the directory are added to the repo in individual commits, resulting in more writes and slower performance:

````
$ for i in {1..10}; do curl performance-test.storage-performance.svc.cluster.local:8080; done
````

Alternatively, to copy all files to the directory, but add them in a single commit at the end:

````
$ for i in {1..10}; do curl performance-test.storage-performance.svc.cluster.local:8080/?multiCommit=false; done
````

Look at the pod log to see the performance:

````
$ oc get pods -o name | xargs -I pod oc logs pod
````

To switch to a persistent volume claim separately created and called "storage-test", follow up with these commands:

````
$ oc volume dc/performance-test --add --name=storage-test --type=persistentVolumeClaim --claim-name=storage-test --mount-path=/deployments/data
````

Then test again and compare numbers.
