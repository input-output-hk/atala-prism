# PRISM Kotlin SDK JavaScript Example
To run this example, first, you have to build PRISM Kotlin SDK located in the top-level repository folder `prism-kotlin-sdk`:
```
$ ./gradlew compileProductionLibraryKotlinJs
```

The build process generates a CommonJS module located in `prism-kotlin-sdk/build/js`. It has a few packages which you can use individually in your project. This particular example makes use of all of them.

Next, you need to install all yarn dependencies:
```
$ yarn install
```

Finally, run the example by typing the command below:

```
$ yarn start
```
