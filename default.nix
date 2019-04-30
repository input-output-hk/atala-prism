{ system ? builtins.currentSystem
, crossSystem ? null
, config ? {}
, nixpkgs ? import ./nix/nixpkgs.nix
, pkgs ? import nixpkgs {inherit system crossSystem config;}
}:
with pkgs;
let commonAttrs = {
      postPatch = ''
        # Configure Bazel to use JDK8
        cat >> .bazelrc <<EOF
        build --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8
        build --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8
        build --host_javabase=@local_jdk//:jdk
        build --javabase=@local_jdk//:jdk
        EOF
        '';
      preConfigure = ''export JAVA_HOME="${jre.home}"'';
      src = nix-gitignore.gitignoreSource [] ./.;
    };

in {
  atala-jars = buildBazelPackage rec {
    inherit (commonAttrs) src;
    name = "atala-jars";
    meta = with stdenv.lib; {
      homepage = "https://github.com/input-output-hk/atala";
      description = "";
      license = licenses.mit;
      platforms = platforms.all;
    };
    bazelTarget = "//main/io/iohk/cef/main:main_deploy.jar";
    fetchAttrs.sha256 = "1b50xymgkc80260j5b2wrw3d4h4klz53jchci1zx9m9gr92bphwy";
    buildInputs = [ git ];
    buildAttrs = {
      inherit (commonAttrs) postPatch preConfigure;
      installPhase = ''
        mkdir -p $out/bin/io/iohk/
        cp -r bazel-bin/main/io/iohk/cef/ $out/bin/io/iohk/cef
      '';
    };
  };

  atala-identity-ledger = buildBazelPackage rec {
  inherit (commonAttrs) src;
    name = "atala-identity-ledger";
    meta = with stdenv.lib; {
      homepage = "https://github.com/input-output-hk/atala";
      description = "";
      license = licenses.mit;
      platforms = platforms.all;
    };
    bazelTarget = "//main/io/iohk/cef/main:main";
    fetchAttrs.sha256 = "1b50xymgkc80260j5b2wrw3d4h4klz53jchci1zx9m9gr92bphwy";
    buildInputs = [ git ];
    buildAttrs = {
      inherit (commonAttrs) postPatch preConfigure;
      installPhase = ''
        #
        # We need to move the runfiles to the bazel-bin directory
        # if we want to keep them in the store. By default, these links
        # are pointing to the `nix-build` temporary directory...
        #
        # We are replacing the links to `bazel-out` by their actual
        # files basically.
        #
        replace-link() {
            # This function is used to replace the runfiles links by their
            # actual files.
            cp --remove-destination $(readlink "$1") "$1"
        };
        export -f replace-link
        find bazel-bin/main/io/iohk/cef/main/main.runfiles -type l -exec ${bash}/bin/bash -c \
             'replace-link {} "$0"' {} \;

        mkdir -p $out/bin
        cp -r bazel-bin/main/io/iohk/cef/main/* $out/bin
        '';
    };
  };
}
