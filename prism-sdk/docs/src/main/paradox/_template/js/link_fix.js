function sourceUrlFix(sourceUrl) {
    // TODO: Use any branch instead of develop
    $("#source-link").attr("href", sourceUrl.replace("tree/master/docs/target/mdoc", "tree/develop/prism-sdk/docs/src/main/paradox"))
}
