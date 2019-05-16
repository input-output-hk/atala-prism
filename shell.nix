{ config ? {},
  nixpkgs ? import ./nix/nixpkgs.nix,
  pkgs ? import nixpkgs {inherit config;}
} :

pkgs.mkShell {
    buildInputs = with pkgs; [
        bazel
    ];

    shellHook = ''
        export JAVA_HOME="${pkgs.jre.home}"
        BAZELRC_LOCAL=".bazelrc.local"
        if [ ! -e "$BAZELRC_LOCAL" ]
        then
            echo "[!] It looks like you are using a nix-based system. In order to build this project, you probably need to add the two following lines your .bazelrc.local file."
            echo ""

            echo "build --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8"
            echo "build --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8"
            echo "build --host_javabase=@local_jdk//:jdk"
            echo "build --javabase=@local_jdk//:jdk"
            echo ""
        fi

        # source bazel bash completion
        source ${pkgs.bazel}/share/bash-completion/completions/bazel
    '';
}
