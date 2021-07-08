package io.iohk.atala.prism.app.neo.common

val softCardanoAddressRegex = "^(addr|addr_test)?1[a-zA-HJ-NP-Z0-9]{25,110}\$".toRegex()

val softCardanoExtendedPublicKeyRegex = "^acct_xvk?1[a-zA-HJ-NP-Z0-9]{25,150}\$".toRegex()

fun softCardanoAddressValidation(address: String): Boolean = softCardanoAddressRegex.matches(address)

fun softCardanoExtendedPublicKeyValidation(extendedPublicKey: String): Boolean = softCardanoExtendedPublicKeyRegex.matches(extendedPublicKey)
