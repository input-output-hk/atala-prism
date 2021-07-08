package io.iohk.atala

import io.iohk.atala.prism.app.neo.common.softCardanoAddressValidation
import io.iohk.atala.prism.app.neo.common.softCardanoExtendedPublicKeyValidation
import org.junit.Assert
import org.junit.Test

class RegexUtilsTest {

    @Test
    fun testSoftCardanoAddressValidation() {
        // Valid cardano addresses
        Assert.assertTrue(softCardanoAddressValidation("addr1qyejptd8yktmsuv24dkd5y2e7xnankz9p4nla98q6e5hyt43hex9jwaselax0v6td0vfjlxdp2xyl6e7swzjn853k7qshgxpv6"))
        Assert.assertTrue(softCardanoAddressValidation("addr1qx8m9wujhq5edhw85zfs38mpzakzdv0fdpa9kk6jgwkjt793hex9jwaselax0v6td0vfjlxdp2xyl6e7swzjn853k7qsj87exn"))
        Assert.assertTrue(softCardanoAddressValidation("addr1qxtfyu7qqkllm624eq2zcag3fqgxr0qrllpzuqqrp2e8ev93hex9jwaselax0v6td0vfjlxdp2xyl6e7swzjn853k7qs7sjrud"))
        // Invalid cardano addresses
        Assert.assertFalse(softCardanoAddressValidation(""))
        Assert.assertFalse(softCardanoAddressValidation("acct_xvk1scz0a95a94cdp99n6spqzen2x0uvqrxhkxqhth0f5gcged0ua3wmvw33kuavrznuzqzwswkrnkcfujzc5kuxptzdj5jp29nmh7c28dg0kkdy5"))
        Assert.assertFalse(softCardanoAddressValidation("bc1quzqafa7xsf0zekx7l7vteskqtz9wnatpatw9g2"))
        Assert.assertFalse(softCardanoAddressValidation("0x65B0826AA19e8Fe45Ef9b109D1c81B6Ae637AAc7"))
    }

    @Test
    fun testSoftCardanoExtendedPublicKeyValidation() {
        // Valid Cardano Extended Public Key´s
        Assert.assertTrue(softCardanoExtendedPublicKeyValidation("acct_xvk1scz0a95a94cdp99n6spqzen2x0uvqrxhkxqhth0f5gcged0ua3wmvw33kuavrznuzqzwswkrnkcfujzc5kuxptzdj5jp29nmh7c28dg0kkdy5"))
        Assert.assertTrue(softCardanoExtendedPublicKeyValidation("acct_xvk17mjg0a5nfcgflppwapprv4q60jrxagnj9zvssxrlj3auhnks99qgw2jqkaut78qewqtp88spa7yf8p5u9yttlgts36qh5emlu6hrwrgh3f70a"))
        // Invalid Cardano Extended Public Key´s
        Assert.assertFalse(softCardanoExtendedPublicKeyValidation(""))
        Assert.assertFalse(softCardanoExtendedPublicKeyValidation("addr1qyejptd8yktmsuv24dkd5y2e7xnankz9p4nla98q6e5hyt43hex9jwaselax0v6td0vfjlxdp2xyl6e7swzjn853k7qshgxpv6"))
        Assert.assertFalse(softCardanoExtendedPublicKeyValidation("bc1quzqafa7xsf0zekx7l7vteskqtz9wnatpatw9g2"))
        Assert.assertFalse(softCardanoExtendedPublicKeyValidation("0x65B0826AA19e8Fe45Ef9b109D1c81B6Ae637AAc7"))
    }
}
