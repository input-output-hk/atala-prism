//
import JavaScriptCore
import WebKit
import CommonCrypto
import crypto

class CryptoUtils: NSObject {

    public static let ec = EC()
    public static let sha256Digest = SHA256Digest.Companion()
    public static let keyDerivation = KeyDerivation()
    public static let derivationPath = DerivationPath.Companion()

    static let global = CryptoUtils()

    static let SEED_COUNT = 12

    var usedMnemonics: [String]?
    var seed: KotlinByteArray?
    var lastUsedKeyIndex: Int?

    let signSemaphore = DispatchSemaphore(value: 1)

    override init() {
        super.init()
        self.lastUsedKeyIndex = SharedMemory.global.loggedUser?.lastUsedKeyIndex
        self.usedMnemonics = SharedMemory.global.loggedUser?.mnemonics
        if usedMnemonics != nil {
            self.generateSeed(mnemonics: MnemonicCode(words: usedMnemonics!))
        }
    }

    func setupMnemonics() {
        let mnemonics = CryptoUtils.keyDerivation.randomMnemonicCode()
        usedMnemonics = mnemonics.words

        generateSeed(mnemonics: mnemonics)
    }

    func generateSeed(mnemonics: MnemonicCode) {
        seed = CryptoUtils.keyDerivation.binarySeed(seed: mnemonics, passphrase: "")
    }

    func getNextPublicKeyPath() -> String {
        let index = lastUsedKeyIndex ?? -1
        return "m/\(index + 1)'/0'/0'"
    }

    func confirmNewKeyUsed() -> String {
        let index = lastUsedKeyIndex ?? -1
        lastUsedKeyIndex = index + 1
        if let user = SharedMemory.global.loggedUser {
            user.lastUsedKeyIndex = lastUsedKeyIndex
            SharedMemory.global.loggedUser? = user
        }
        return "m/\(lastUsedKeyIndex!)'/0'/0'"
    }

    func encodedPublicKey(keyPath: String) -> Io_Iohk_Atala_Prism_Protos_EncodedPublicKey {
        let derivationPath = DerivationPath.Companion.init().fromPath(path: keyPath)
        let key = CryptoUtils.keyDerivation.deriveKey(seed: seed!, path: derivationPath)
        let pointData = key.publicKey().getEncoded()
        var protoPublicKey: Io_Iohk_Atala_Prism_Protos_EncodedPublicKey = Io_Iohk_Atala_Prism_Protos_EncodedPublicKey()
        protoPublicKey.publicKey = fromKotlinBytes(bytes: pointData)
        return protoPublicKey
    }

    /// Returns a tuple (Signature, PublicKey, Nonce) all three fields as URL safe base 64 encoded strings
    func signData(data: Data, keyPath: String) -> (String, String, String)? {
        signSemaphore.wait()
        let nonce = UUID()
        var nonceData = withUnsafePointer(to: nonce.uuid) {
            Data(bytes: $0, count: MemoryLayout.size(ofValue: nonce.uuid))
        }
        let nonceBase64 = nonceData.base64urlEncodedString()
        nonceData.append(data)
        let path = CryptoUtils.derivationPath.fromPath(path: keyPath)
        let derived = CryptoUtils.keyDerivation.deriveKey(seed: seed!, path: path)
        let privateKey = derived.privateKey()
        let publicKey = derived.publicKey()
        let publicKeyData = fromKotlinBytes(bytes: publicKey.getEncoded())
        let signature = CryptoUtils.ec.sign(data: toKotlinBytes(data: nonceData), privateKey: privateKey)
        let signedData = fromKotlinBytes(bytes: signature.getEncoded())
        signSemaphore.signal()
        return (signedData.base64urlEncodedString(), publicKeyData.base64urlEncodedString(), nonceBase64)
    }

    func getUsedRandomIndexes(count: Int) -> [Int] {
        let indexes = Array(0 ..< CryptoUtils.SEED_COUNT)
        return Array(indexes.randomPick(count)).sorted()
    }

    func checkWordsValidity(indexes: [Int], words: [String]) -> Bool {
        for pos in 0 ..< indexes.count where usedMnemonics![indexes[pos]] != words[pos].lowercased() {
            return false
        }
        return true
    }
    
    func isValidShelleyAddress(address: String) -> Bool {
        
        let range = address.range(of: #"addr(?:_test)?1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+"#,
                                     options: .regularExpression)
        return range != nil
    }
    
    func isValidExtendedPublicKey(key: String) -> Bool {
        
        let range = key.range(of: #"acct_xvk1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+"#,
                                     options: .regularExpression)
        return range != nil
    }
}
