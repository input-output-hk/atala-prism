//

class CryptoUtils {

    static func getPublicKey(privateKey: String) {

        let ecpk = toECPrivateKey(d: privateKey)
    }

    static func toECPrivateKey(d: String) -> Io_Iohk_Cvp_Wallet_ECPrivateKey {
        var ecpk = Io_Iohk_Cvp_Wallet_ECPrivateKey()
        var bi = Io_Iohk_Cvp_Wallet_BigInteger()
        bi.value = d
        ecpk.d = bi
        return ecpk
    }
}
