# cardano-enterprise
The Cardano Enterprise Framework and Reference Applications

[![CircleCI](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/experiment%2Fbase-network.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)

#### Working with the codebase

To be able to build or test the codebase, `sbt-verify` needs to be properly setup.

`sbt-verify` is used in order to check the validity of the checksums of all the downloaded libraries.

`sbt-verify` can be downloaded from our read only repository by typing

 `git clone  https://github.com/input-output-hk/sbt-verify`

Then in order to make `sbt-verify` available to our build type

```
cd sbt-verify
git checkout sbt-1.x
sbt publishLocal
```

This installs the `sbt-verify` library to your local repository.

After installing the `sbt-verify` library to your local repository checkout this repository from github and, for example, type

```
cd cef/network
sbt test`
```

in the root of the project.


