load("//scala:rules.bzl", "scala_binary")


shared_deps = [
  "//:cef",
  "//main/io/iohk/cef/frontend",
]



scala_binary(
  name = "main",
  deps = shared_deps,
  main_class = "io.iohk.cef.main.IdentityTxMain",
)

scala_binary(
  name = "AgreementsMain",
  deps = shared_deps,
  main_class = "io.iohk.cef.main.AgreementsMain",
)

scala_binary(
  name = "ChimericMain",
  deps = shared_deps,
  main_class = "io.iohk.cef.main.ChimericMain",
)
