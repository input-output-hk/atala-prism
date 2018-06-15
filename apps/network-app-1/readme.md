### Running the demo node and its performance test

* Run publishLocal against the network module 
```bash
$ cd cardano-enterprise/cef/network 
$ sbt publishLocal
```
* startup some demo nodes
```bash 
$ cd cardano-enterprise/apps/network-app-1
$ sbt -J-Dconfig.resource=node-a.conf 'node-server/runMain io.iohk.cef.NetworkApp1'
$ sbt -J-Dconfig.resource=node-b.conf 'node-server/runMain io.iohk.cef.NetworkApp1'
$ sbt -J-Dconfig.resource=node-c.conf 'node-server/runMain io.iohk.cef.NetworkApp1'
```
* As an alternative, you can assemble the node into a runnable jar, a la
```bash
$ sbt node-server/assembly
$ java -Dconfig.file=<path/file name> -jar node-server/target/ 
``` 
* run the gatling test
```bash
$ sbt perf-test/gatling-it:test
```
