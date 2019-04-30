# Fix for https://github.com/bazelbuild/rules_scala/issues/726

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_JAVA_VERSION = "3.6.1"

def protobuf_dep():
    http_archive(
        name = "com_google_protobuf",
        url = "http://central.maven.org/maven2/com/google/protobuf/protobuf-java/%s/protobuf-java-%s.jar" % (PROTOBUF_JAVA_VERSION, PROTOBUF_JAVA_VERSION),
        sha256 = "fb66d913ff0578553b2e28a3338cbbbe2657e6cfe0e98d939f23aea219daf508",
        build_file_content =
"""package(default_visibility = ["//visibility:public"])

filegroup(
    name = "meta",
    srcs = glob([
        "META-INF/**/*",
    ])
)


filegroup(
    name = "classes",
    srcs = glob([
        "com/**/*",
        "google/**/*",
    ])
)

genrule(
    name = "protobuf_java",
    srcs = [":meta", ":classes"],
    outs = ["myjar.jar"],
    toolchains = ["@bazel_tools//tools/jdk:current_java_runtime"],
    cmd = "mkdir -p META-INF && mkdir -p com/google/protobuf && mv $(locations :meta) META-INF && mv $(locations :classes) com/google/protobuf && $(JAVABASE)/bin/jar cfM $@ META-INF com",
)""",
    )
