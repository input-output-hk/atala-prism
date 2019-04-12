load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_scala_version="b537bddc58a77318b34165812a0311ef52806318"
http_archive(
             name = "io_bazel_rules_scala",
             url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip"%rules_scala_version,
             type = "zip",
             strip_prefix= "rules_scala-%s" % rules_scala_version
             )

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
scala_repositories(("2.12.7", {
    "scala_compiler": "6e80ef4493127214d31631287a6789170bf6c9a771d6094acd8dc785e8970270",
    "scala_library": "8f3dc6091db688464ad8b1ee6c7343d7aa5940d474ee8b90406c71e45dd74fc0",
    "scala_reflect": "7427d7ee5771e8c36c1db5a09368fa3078f6eceb77d7c797a322a088c5dddb76"
}))

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
register_toolchains("//toolchains:cef_scala_toolchain")

load("//3rdparty:workspace.bzl", "maven_dependencies")

maven_dependencies()


load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "decco",
    remote = "https://github.com/input-output-hk/decco.git",
    commit = "6ed968d4b3599f104b417fa26f9cf4505e89e35e"
)

git_repository(
    name = "multicrypto",
    remote = "https://github.com/input-output-hk/multicrypto.git",
    commit = "e38504ea42923d572d6c37fbe5077e7d1ae7dce0"
)

git_repository(
    name = "codecs",
    remote = "https://github.com/input-output-hk/decco.git",
    commit = "3232f341ae0695d576ecc20746dffccbb7f120b1"
)

git_repository(
    name = "crypto",
    remote = "https://github.com/input-output-hk/multicrypto.git",
    commit = "d957f4f43e8bfbf767805c1ceda6b8035cb22c33"
)

git_repository(
    name = "network",
    remote = "https://github.com/input-output-hk/scalanet.git",
    commit = "2417be602ce76cc420f5f8acf8b71ac4c902216a"
)
