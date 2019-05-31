load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_scala_version = "ca5a7acff4ff630f68f58b8e01e8c25dbf908fb7"

http_archive(
    name = "io_bazel_rules_scala",
    strip_prefix = "rules_scala-%s" % rules_scala_version,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
)

http_archive(
    name = "com_google_protobuf",
    sha256 = "9510dd2afc29e7245e9e884336f848c8a6600a14ae726adb6befdb4f786f0be2",
    strip_prefix = "protobuf-3.6.1.3",
    urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.6.1.3.zip"],
)

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories((
    "2.12.7",
    {
        "scala_compiler": "6e80ef4493127214d31631287a6789170bf6c9a771d6094acd8dc785e8970270",
        "scala_library": "8f3dc6091db688464ad8b1ee6c7343d7aa5940d474ee8b90406c71e45dd74fc0",
        "scala_reflect": "7427d7ee5771e8c36c1db5a09368fa3078f6eceb77d7c797a322a088c5dddb76",
    },
))

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

register_toolchains("//toolchains:cef_scala_toolchain")

load("//3rdparty:workspace.bzl", "maven_dependencies")

maven_dependencies()

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

# Legacy cardano-enterprise dependencies
git_repository(
    name = "codecs",
    commit = "e89981d41dbfc1108596c4e9c871c58c1f58dfab",
    remote = "https://github.com/input-output-hk/decco.git",
)

git_repository(
    name = "crypto",
    commit = "3301f61e07877348ee20186f4759f5fe8736e77e",
    remote = "https://github.com/input-output-hk/multicrypto.git",
)

git_repository(
    name = "network-cardano-enterprise",
    commit = "2417be602ce76cc420f5f8acf8b71ac4c902216a",
    remote = "https://github.com/input-output-hk/scalanet.git",
)

# Atala dependencies
git_repository(
    name = "decco",
    commit = "e89981d41dbfc1108596c4e9c871c58c1f58dfab",
    remote = "https://github.com/input-output-hk/decco.git",
)

git_repository(
    name = "multicrypto",
    commit = "3301f61e07877348ee20186f4759f5fe8736e77e",
    remote = "https://github.com/input-output-hk/multicrypto.git",
)

git_repository(
    name = "scalanet",
    commit = "06cfdd3baf0980369fdd61fe5457b6782ea5d60a",
    remote = "https://github.com/input-output-hk/scalanet.git",
)

load("//bazel_tools:java.bzl", "java_home_runtime")

java_home_runtime(name = "java_home")

load("//bazel_tools:protobuf.bzl", "protobuf_dep")

protobuf_dep()
