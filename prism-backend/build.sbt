lazy val prismBackend = PrismBuild.prism

lazy val common = PrismBuild.common
lazy val node = PrismBuild.node
lazy val connector = PrismBuild.connector
lazy val keyderivation = PrismBuild.keyderivation
lazy val vault = PrismBuild.vault
lazy val managementConsole = PrismBuild.managementConsole

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / pushRemoteCacheTo := (
  if (sys.env.contains("CI"))
    Some(MavenCache("local-cache", file("/tmp/backend-remote-cache")))
  else None
)
