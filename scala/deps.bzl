def maven_dependencies():
    native.maven_jar(
        name = "ch_qos_logback_logback_classic",
        artifact = "ch.qos.logback:logback-classic:1.2.3",
        sha1 = "7c4f3c474fb2c041d8028740440937705ebb473a",
    )
    native.maven_jar(
        name = "ch_qos_logback_logback_core",
        artifact = "ch.qos.logback:logback-core:1.2.3",
        sha1 = "864344400c3d4d92dfeb0a305dc87d953677c03c",
    )
    native.maven_jar(
        name = "com_alexitc_playsonify_akka_http_2_12",
        artifact = "com.alexitc:playsonify-akka-http_2.12:2.0.0-RC0",
        sha1 = "6e613e95917cb759effe086c3af03d47668ddceb",
    )
    native.maven_jar(
        name = "com_alexitc_playsonify_core_2_12",
        artifact = "com.alexitc:playsonify-core_2.12:2.0.0-RC0",
        sha1 = "6e238ea32d591f74b5710371c6e54eb1107dca80",
    )
    native.maven_jar(
        name = "com_beachape_enumeratum_macros_2_12",
        artifact = "com.beachape:enumeratum-macros_2.12:1.5.9",
        sha1 = "1a63056f0ba55a11c8d10150d27c247920e6c9c8",
    )
    native.maven_jar(
        name = "com_beachape_enumeratum_2_12",
        artifact = "com.beachape:enumeratum_2.12:1.5.13",
        sha1 = "f3cd444af103422b9baa0f81bf2f55d6e0378546",
    )
    native.maven_jar(
        name = "com_chuusai_shapeless_2_12",
        artifact = "com.chuusai:shapeless_2.12:2.3.3",
        sha1 = "6041e2c4871650c556a9c6842e43c04ed462b11f",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:2.9.6",
        sha1 = "6a0f0f154edaba00067772ce02e24f8c0973d84c",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.9.6",
        sha1 = "4e393793c37c77e042ccc7be5a914ae39251b365",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_databind",
        artifact = "com.fasterxml.jackson.core:jackson-databind:2.9.6",
        sha1 = "cfa4f316351a91bfd95cb0644c6a2c95f52db1fc",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_dataformat_jackson_dataformat_yaml",
        artifact = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.6",
        sha1 = "2cf807d8a1a6a52e80a269e73b2fd8c0df06a42b",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_datatype_jackson_datatype_jdk8",
        artifact = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.8.11",
        sha1 = "5c897945f0af08f6432b96c17746317159ec322b",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_datatype_jackson_datatype_jsr310",
        artifact = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.8.11",
        sha1 = "6d8fbd79634b348d1f1ab99a13da28a5717049e6",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_module_jackson_module_paranamer",
        artifact = "com.fasterxml.jackson.module:jackson-module-paranamer:2.9.6",
        sha1 = "cafa3a01569735a965b4c25ec3ba40aa9118e405",
    )
    native.maven_jar(
        name = "com_fasterxml_jackson_module_jackson_module_scala_2_12",
        artifact = "com.fasterxml.jackson.module:jackson-module-scala_2.12:2.9.6",
        sha1 = "697083434cb4df855d42f1a77d8c903f74545b34",
    )
    native.maven_jar(
        name = "com_github_sbtourist_journalio",
        artifact = "com.github.sbtourist:journalio:1.4.2",
        sha1 = "ed4eb122ef5393825d558ecb028bf83b3f13e2e7",
    )
    native.maven_jar(
        name = "com_github_swagger_akka_http_swagger_akka_http_2_12",
        artifact = "com.github.swagger-akka-http:swagger-akka-http_2.12:1.0.0",
        sha1 = "49eedd643fd11edc41a21a069c8ba9e9cd976539",
    )
    native.maven_jar(
        name = "com_google_guava_guava",
        artifact = "com.google.guava:guava:20.0",
        sha1 = "89507701249388e1ed5ddcf8c41f4ce1be7831ef",
    )
    native.maven_jar(
        name = "com_h2database_h2",
        artifact = "com.h2database:h2:1.4.197",
        sha1 = "bb391050048ca8ae3e32451b5a3714ecd3596a46",
    )
    native.maven_jar(
        name = "com_netflix_spectator_spectator_api",
        artifact = "com.netflix.spectator:spectator-api:0.57.1",
        sha1 = "2b9d5d2735d6422ae49d6270bdcd63f373996026",
    )
    native.maven_jar(
        name = "com_softwaremill_quicklens_quicklens_2_12",
        artifact = "com.softwaremill.quicklens:quicklens_2.12:1.4.11",
        sha1 = "89e9ca901795e349f69fa2955919860f76cbe954",
    )
    native.maven_jar(
        name = "com_thoughtworks_paranamer_paranamer",
        artifact = "com.thoughtworks.paranamer:paranamer:2.8",
        sha1 = "619eba74c19ccf1da8ebec97a2d7f8ba05773dd6",
    )
    native.maven_jar(
        name = "com_typesafe_config",
        artifact = "com.typesafe:config:1.3.3",
        sha1 = "4b68c2d5d0403bb4015520fcfabc88d0cbc4d117",
    )
    native.maven_jar(
        name = "com_typesafe_ssl_config_core_2_12",
        artifact = "com.typesafe:ssl-config-core_2.12:0.2.3",
        sha1 = "c1fdf6c0e7af6b5dd1df14e38062f917320413b4",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_actor_typed_2_12",
        artifact = "com.typesafe.akka:akka-actor-typed_2.12:2.5.12",
        sha1 = "072efe632266e9934bca5ce130b0c8d7da371b6e",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_actor_2_12",
        artifact = "com.typesafe.akka:akka-actor_2.12:2.5.14",
        sha1 = "9e8c4db1824b41e09119e44ac74988d7230d8154",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_http_core_2_12",
        artifact = "com.typesafe.akka:akka-http-core_2.12:10.1.5",
        sha1 = "d0c4f29673b042a16a6255f024a1129c71940bc1",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_http_spray_json_2_12",
        artifact = "com.typesafe.akka:akka-http-spray-json_2.12:10.1.4",
        sha1 = "8d8e887567a6ad0b6a7d38e0794bccd57406cfea",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_http_testkit_2_12",
        artifact = "com.typesafe.akka:akka-http-testkit_2.12:10.1.4",
        sha1 = "02310849631a811d44a6d6e508c390d581ba92b7",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_http_2_12",
        artifact = "com.typesafe.akka:akka-http_2.12:10.1.5",
        sha1 = "53acfbbde125087f9929b018e621bec1d84e4511",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_parsing_2_12",
        artifact = "com.typesafe.akka:akka-parsing_2.12:10.1.5",
        sha1 = "fbd143597a0363deebba57fb11d9494e35f4b976",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_protobuf_2_12",
        artifact = "com.typesafe.akka:akka-protobuf_2.12:2.5.14",
        sha1 = "4d35b3e57819a9ab3a1745654410d2d522eed7e0",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_slf4j_2_12",
        artifact = "com.typesafe.akka:akka-slf4j_2.12:2.5.12",
        sha1 = "985acf0acbf0cf6b1222754e5818709c90024857",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_stream_testkit_2_12",
        artifact = "com.typesafe.akka:akka-stream-testkit_2.12:2.5.12",
        sha1 = "b96e9a1e45fb4a2785ab94c1c01357265e3f01f7",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_stream_2_12",
        artifact = "com.typesafe.akka:akka-stream_2.12:2.5.14",
        sha1 = "981f769626486d30a1153299982cde2bd2c6035d",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_testkit_typed_2_12",
        artifact = "com.typesafe.akka:akka-testkit-typed_2.12:2.5.12",
        sha1 = "27c96dd32eb4aeb2e0c40afa9f71b5bbbc348d2d",
    )
    native.maven_jar(
        name = "com_typesafe_akka_akka_testkit_2_12",
        artifact = "com.typesafe.akka:akka-testkit_2.12:2.5.12",
        sha1 = "b2ce3dfed156bc6bd46dac040ae1a0c39900f852",
    )
    native.maven_jar(
        name = "com_typesafe_play_play_functional_2_12",
        artifact = "com.typesafe.play:play-functional_2.12:2.6.10",
        sha1 = "79e9914ed9f987aa38ff99b88478bf4cfe82b15f",
    )
    native.maven_jar(
        name = "com_typesafe_play_play_json_2_12",
        artifact = "com.typesafe.play:play-json_2.12:2.6.10",
        sha1 = "99663df4313777df1fa6e163d2edc3ef8bfbee37",
    )
    native.maven_jar(
        name = "com_zaxxer_HikariCP",
        artifact = "com.zaxxer:HikariCP:3.1.0",
        sha1 = "6eda96598aa9d4c657ecfc3dfe88307d5fdb4b78",
    )
    native.maven_jar(
        name = "commons_io_commons_io",
        artifact = "commons-io:commons-io:2.6",
        sha1 = "815893df5f31da2ece4040fe0a12fd44b577afaf",
    )
    native.maven_jar(
        name = "commons_logging_commons_logging",
        artifact = "commons-logging:commons-logging:1.2",
        sha1 = "4bfc12adfe4842bf07b657f0369c4cb522955686",
    )
    native.maven_jar(
        name = "de_heikoseeberger_akka_http_play_json_2_12",
        artifact = "de.heikoseeberger:akka-http-play-json_2.12:1.22.0",
        sha1 = "a6edcb09a55e8ab615081c1a6851d43a5bd2bc50",
    )
    native.maven_jar(
        name = "io_iohk_cef_network_2_12",
        artifact = "io.iohk.cef:network_2.12:0.1-SNAPSHOT",
    )
    native.maven_jar(
        name = "io_micrometer_micrometer_core",
        artifact = "io.micrometer:micrometer-core:0.12.0.RELEASE",
        sha1 = "70f5fc79943d93fcfcb7e1576df7574537bc0004",
    )
    native.maven_jar(
        name = "io_micrometer_micrometer_registry_datadog",
        artifact = "io.micrometer:micrometer-registry-datadog:0.12.0.RELEASE",
        sha1 = "41d390d942a830efd701d5784a114769ded5ae0a",
    )
    native.maven_jar(
        name = "io_monix_monix_eval_2_12",
        artifact = "io.monix:monix-eval_2.12:3.0.0-RC1",
        sha1 = "093f340f8b3c70e53ed10820e9bc637a884ef2d5",
    )
    native.maven_jar(
        name = "io_monix_monix_execution_2_12",
        artifact = "io.monix:monix-execution_2.12:3.0.0-RC1",
        sha1 = "c35e304152b7f2fa57575452601fa81dab90b657",
    )
    native.maven_jar(
        name = "io_monix_monix_java_2_12",
        artifact = "io.monix:monix-java_2.12:3.0.0-RC1",
        sha1 = "f1f80eaf501f2122809dad987c7e6dfc9dc3deff",
    )
    native.maven_jar(
        name = "io_monix_monix_reactive_2_12",
        artifact = "io.monix:monix-reactive_2.12:3.0.0-RC1",
        sha1 = "1f507913d48a8273b835e5a2a679a5d1e80153db",
    )
    native.maven_jar(
        name = "io_monix_monix_tail_2_12",
        artifact = "io.monix:monix-tail_2.12:3.0.0-RC1",
        sha1 = "598d24fd7cbb00d421e9217a0869438f34cf51c9",
    )
    native.maven_jar(
        name = "io_monix_monix_2_12",
        artifact = "io.monix:monix_2.12:3.0.0-RC1",
        sha1 = "e252ab4de5ba4bb1afbde3103739e8750f620311",
    )
    native.maven_jar(
        name = "io_netty_netty_all",
        artifact = "io.netty:netty-all:4.1.28.Final",
        sha1 = "33ae3d109e16b8c591bdf343f6b52ccd0ef75905",
    )
    native.maven_jar(
        name = "io_spray_spray_json_2_12",
        artifact = "io.spray:spray-json_2.12:1.3.4",
        sha1 = "0f9e8157b41b2e1fffa52fca445ce4571e79259b",
    )
    native.maven_jar(
        name = "io_swagger_swagger_annotations",
        artifact = "io.swagger:swagger-annotations:1.5.20",
        sha1 = "16051f93ce11ca489a5313775d825f82fcc2cd6c",
    )
    native.maven_jar(
        name = "io_swagger_swagger_core",
        artifact = "io.swagger:swagger-core:1.5.20",
        sha1 = "803903905bc32681e858457998533cf3021686c9",
    )
    native.maven_jar(
        name = "io_swagger_swagger_jaxrs",
        artifact = "io.swagger:swagger-jaxrs:1.5.20",
        sha1 = "ec9df2a00918dcf4f688cafc9818a2a3f1afd068",
    )
    native.maven_jar(
        name = "io_swagger_swagger_models",
        artifact = "io.swagger:swagger-models:1.5.20",
        sha1 = "fb3a23bad80c5ed84db9dd150db2cba699531458",
    )
    native.maven_jar(
        name = "javax_validation_validation_api",
        artifact = "javax.validation:validation-api:1.1.0.Final",
        sha1 = "8613ae82954779d518631e05daa73a6a954817d5",
    )
    native.maven_jar(
        name = "javax_ws_rs_jsr311_api",
        artifact = "javax.ws.rs:jsr311-api:1.1.1",
        sha1 = "59033da2a1afd56af1ac576750a8d0b1830d59e6",
    )
    native.maven_jar(
        name = "joda_time_joda_time",
        artifact = "joda-time:joda-time:2.9.9",
        sha1 = "f7b520c458572890807d143670c9b24f4de90897",
    )
    native.maven_jar(
        name = "net_bytebuddy_byte_buddy",
        artifact = "net.bytebuddy:byte-buddy:1.8.15",
        sha1 = "cb36fe3c70ead5fcd016856a7efff908402d86b8",
    )
    native.maven_jar(
        name = "net_bytebuddy_byte_buddy_agent",
        artifact = "net.bytebuddy:byte-buddy-agent:1.8.15",
        sha1 = "a2dbe3457401f65ad4022617fbb3fc0e5f427c7d",
    )
    native.maven_jar(
        name = "org_apache_commons_commons_dbcp2",
        artifact = "org.apache.commons:commons-dbcp2:2.2.0",
        sha1 = "88763be2c54a37dd0b2d5d735d268bd6a202a207",
    )
    native.maven_jar(
        name = "org_apache_commons_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.2.1",
        sha1 = "66f13681add50ca9e4546ffabafaaac7645db3cf",
    )
    native.maven_jar(
        name = "org_apache_commons_commons_pool2",
        artifact = "org.apache.commons:commons-pool2:2.5.0",
        sha1 = "cb7d05e6308ad795decc4a12ede671113c11dd98",
    )
    native.maven_jar(
        name = "org_bouncycastle_bcprov_jdk15on",
        artifact = "org.bouncycastle:bcprov-jdk15on:1.59",
        sha1 = "2507204241ab450456bdb8e8c0a8f986e418bd99",
    )
    native.maven_jar(
        name = "org_flywaydb_flyway_core",
        artifact = "org.flywaydb:flyway-core:5.1.3",
        sha1 = "ce01828cde01a28de19c146607127ef443af6b74",
    )
    native.maven_jar(
        name = "org_javassist_javassist",
        artifact = "org.javassist:javassist:3.21.0-GA",
        sha1 = "598244f595db5c5fb713731eddbb1c91a58d959b",
    )
    native.maven_jar(
        name = "org_jctools_jctools_core",
        artifact = "org.jctools:jctools-core:2.1.1",
        sha1 = "9a1a7e006fb11f64716694c30de243fdf973c62f",
    )
    native.maven_jar(
        name = "org_mockito_mockito_core",
        artifact = "org.mockito:mockito-core:2.21.0",
        sha1 = "cdd1d0d5b2edbd2a7040735ccf88318c031f458b",
    )
    native.maven_jar(
        name = "org_objenesis_objenesis",
        artifact = "org.objenesis:objenesis:2.6",
        sha1 = "639033469776fd37c08358c6b92a4761feb2af4b",
    )
    native.maven_jar(
        name = "org_reactivestreams_reactive_streams",
        artifact = "org.reactivestreams:reactive-streams:1.0.2",
        sha1 = "323964c36556eb0e6209f65c1cef72b53b461ab8",
    )
    native.maven_jar(
        name = "org_reflections_reflections",
        artifact = "org.reflections:reflections:0.9.11",
        sha1 = "4c686033d918ec1727e329b7222fcb020152e32b",
    )
    native.maven_jar(
        name = "org_scala_lang_scala_reflect",
        artifact = "org.scala-lang:scala-reflect:2.12.7",
        sha1 = "c5a8eb12969e77db4c0dd785c104b59d226b8265",
    )
    native.maven_jar(
        name = "org_scala_lang_modules_scala_java8_compat_2_12",
        artifact = "org.scala-lang.modules:scala-java8-compat_2.12:0.8.0",
        sha1 = "1e6f1e745bf6d3c34d1e2ab150653306069aaf34",
    )
    native.maven_jar(
        name = "org_scala_lang_modules_scala_parser_combinators_2_12",
        artifact = "org.scala-lang.modules:scala-parser-combinators_2.12:1.1.0",
        sha1 = "bbce493f8bf61b56623624ff96ac3865f7f6999a",
    )
    native.maven_jar(
        name = "org_scala_lang_modules_scala_xml_2_12",
        artifact = "org.scala-lang.modules:scala-xml_2.12:1.0.6",
        sha1 = "e22de3366a698a9f744106fb6dda4335838cf6a7",
    )
    native.maven_jar(
        name = "org_scala_sbt_test_interface",
        artifact = "org.scala-sbt:test-interface:1.0",
        sha1 = "0a3f14d010c4cb32071f863d97291df31603b521",
    )
    native.maven_jar(
        name = "org_scala_stm_scala_stm_2_12",
        artifact = "org.scala-stm:scala-stm_2.12:0.8",
        sha1 = "1ceeedf00f40697b77a459bdecd451b014960276",
    )
    native.maven_jar(
        name = "org_scalacheck_scalacheck_2_12",
        artifact = "org.scalacheck:scalacheck_2.12:1.14.0",
        sha1 = "8b4354c1a5e1799b4b0ab888d326e7f7fdb02d0d",
    )
    native.maven_jar(
        name = "org_scalactic_scalactic_2_12",
        artifact = "org.scalactic:scalactic_2.12:3.0.5",
        sha1 = "edec43902cdc7c753001501e0d8c2de78394fb03",
    )
    native.maven_jar(
        name = "org_scalatest_scalatest_2_12",
        artifact = "org.scalatest:scalatest_2.12:3.0.5",
        sha1 = "7bb56c0f7a3c60c465e36c6b8022a95b883d7434",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_config_2_12",
        artifact = "org.scalikejdbc:scalikejdbc-config_2.12:3.2.2",
        sha1 = "a90f7dcdaed19d1f85576723a643b5ea92d4d8b7",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_core_2_12",
        artifact = "org.scalikejdbc:scalikejdbc-core_2.12:3.2.2",
        sha1 = "1ca8d0eed8b3f5a12ee4710a59cdb7cc0c2bcaf7",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_interpolation_macro_2_12",
        artifact = "org.scalikejdbc:scalikejdbc-interpolation-macro_2.12:3.2.2",
        sha1 = "655173b995c00d951cb797f965784b10c51d624e",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_interpolation_2_12",
        artifact = "org.scalikejdbc:scalikejdbc-interpolation_2.12:3.2.2",
        sha1 = "f4abb45b1ae936057253291159325969315ef7ff",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_test_2_12",
        artifact = "org.scalikejdbc:scalikejdbc-test_2.12:3.2.2",
        sha1 = "0eb0f6be7327f0758d467ca1b51644206396bb57",
    )
    native.maven_jar(
        name = "org_scalikejdbc_scalikejdbc_2_12",
        artifact = "org.scalikejdbc:scalikejdbc_2.12:3.2.2",
        sha1 = "22eee9668fb7b9e3701a767f23a9dc948491d8dc",
    )
    native.maven_jar(
        name = "org_scoverage_scalac_scoverage_runtime_2_12",
        artifact = "org.scoverage:scalac-scoverage-runtime_2.12:1.3.1",
        sha1 = "b51a70c1da41e07de83506ea12dc25fb994740a5",
    )
    native.maven_jar(
        name = "org_slf4j_slf4j_api",
        artifact = "org.slf4j:slf4j-api:1.7.25",
        sha1 = "da76ca59f6a57ee3102f8f9bd9cee742973efa8a",
    )
    native.maven_jar(
        name = "org_typelevel_cats_core_2_12",
        artifact = "org.typelevel:cats-core_2.12:1.0.1",
        sha1 = "5872b9db29c3e1245f841ac809d5d64b9e56eaa1",
    )
    native.maven_jar(
        name = "org_typelevel_cats_effect_2_12",
        artifact = "org.typelevel:cats-effect_2.12:0.10",
        sha1 = "457a41f51707e64d010520f9d63f7f6a762ff7f5",
    )
    native.maven_jar(
        name = "org_typelevel_cats_kernel_2_12",
        artifact = "org.typelevel:cats-kernel_2.12:1.0.1",
        sha1 = "977ec6bbc1677502d0f6c26beeb0e5ee6c0da0ad",
    )
    native.maven_jar(
        name = "org_typelevel_cats_macros_2_12",
        artifact = "org.typelevel:cats-macros_2.12:1.0.1",
        sha1 = "89374609c1ffe142c7fec887883aff779befb101",
    )
    native.maven_jar(
        name = "org_typelevel_machinist_2_12",
        artifact = "org.typelevel:machinist_2.12:0.6.2",
        sha1 = "a0e8521deafd0d24c18460104eee6ce4c679c779",
    )
    native.maven_jar(
        name = "org_typelevel_macro_compat_2_12",
        artifact = "org.typelevel:macro-compat_2.12:1.1.1",
        sha1 = "ed809d26ef4237d7c079ae6cf7ebd0dfa7986adf",
    )
    native.maven_jar(
        name = "org_yaml_snakeyaml",
        artifact = "org.yaml:snakeyaml:1.18",
        sha1 = "e4a441249ade301985cb8d009d4e4a72b85bf68e",
    )