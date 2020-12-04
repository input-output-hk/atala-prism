W# PRISM
This is the server side from the PRISM project.

## How to import project using BSP
In order to import this project as a BSP project in IntelliJ IDEA, first you need to make sure you have JDK 8 in your `PATH`:
```
$ java -version
openjdk version "1.8.0_265"
OpenJDK Runtime Environment (build 1.8.0_265-b01)
OpenJDK 64-Bit Server VM (build 25.265-b01, mixed mode)
```

Next, you need to generate the necessary `.bsp/sbt.json` file (you can exit sbt as soon as you see `sbt:prism>` greeter):
```
$ sbt -bsp
```

Once the file has been generated, use your favourite text editor to edit `.bsp/sbt.json` and increase `-Xmx` setting to `4096m` so it looks like this:
```
{"name":"sbt","version":"1.4.2","bspVersion":"2.0.0-M5","languages":["scala"],"argv":["/usr/lib/jvm/java-8-openjdk/bin/java","-Xms100m","-Xmx4096m","-classpath","/usr/share/sbt/bin/sbt-launch.jar","xsbt.boot.Boot","-bsp"]}
```

Finally, you can open the project with IntelliJ IDEA (command name may differ depending on your package manager, refer to the IntelliJ IDEA package description in your package manager):
```
$ idea .
```

When IntelliJ IDEA asks you what project configuration to use, pick "Open as BSP project". Once the project has been imported, make sure your project SDK is set to JDK 8.

## Troubleshooting
If you get errors while the project gets imported by IntelliJ, try running IntelliJ from the terminal:
- `./bin/idea.sh` from the IntelliJ directory, for Linux.
- `open -a "IntelliJ IDEA"` or `open -a "IntelliJ IDEA CE"` for Mac.


This occurs commonly when dealing with scalajs, because npm is required, and, sometimes IntelliJ fails to get the proper `PATH`, example error log:

```
[error] (ProjectRef(uri("file:/home/dell/iohk/repo/cardano-enterprise/prism-sdk/"), "sdkJS") / ssExtractDependencies) java.io.IOException: Cannot run program "npm" (in directory "/home/dell/iohk/repo/cardano-enterprise/prism-sdk/js/target/scala-2.13/scalajs-bundler/main"): error=2, No such file or directory
```
