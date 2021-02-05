//
import BitcoinKit
import JavaScriptCore
import WebKit
import CommonCrypto
import crypto

class CryptoUtils: NSObject {

    public static let sha256 = SHA256()

    static let global = CryptoUtils()

    static let SEED_COUNT = 12

    var usedMnemonics: [String]?
    var seed: [KotlinByte]?
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
        let mnemonics = SwiftKeyDerivation.global.randomMnemonicCode()
        usedMnemonics = mnemonics.words

        generateSeed(mnemonics: mnemonics)
    }

    func generateSeed(mnemonics: MnemonicCode) {
        seed = SwiftKeyDerivation.global.binarySeed(seed: mnemonics, passphrase: "")
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
        let key = SwiftKeyDerivation.global.deriveKey(seed: seed!, path: derivationPath)
        let pointData = key.publicKey().getEncoded()
        var protoPublicKey: Io_Iohk_Atala_Prism_Protos_EncodedPublicKey = Io_Iohk_Atala_Prism_Protos_EncodedPublicKey()
        protoPublicKey.publicKey = fromKotlinBytes(bytes: pointData)
        return protoPublicKey
    }

    func getUncompressedKey(publicKey: PublicKey) -> Data {
        let publicPoint = try? PointOnCurve.decodePointFromPublicKey(publicKey)
        if let pointData = NSMutableData(capacity: 65) {
            pointData.append(Data(repeating: 0x04, count: 1))
            pointData.append((publicPoint?.x.data)!)
            pointData.append((publicPoint?.y.data)!)
            return pointData as Data
        }

        return Data()
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
        let nonceSHA256 = Data(CryptoUtils.sha256.compute(bytes: toKotlinBytes(data: nonceData)).map { $0.uint8Value })
        let seedData = fromKotlinBytes(bytes: seed!)
        let keychain = HDKeychain(seed: seedData, network: .testnetBTC)
        let derived = try? keychain.derivedKey(path: keyPath)
        if let privateKey = derived?.privateKey() {
            let signedData = privateKey.sign(nonceSHA256)
            let publicKey = getUncompressedKey(publicKey: privateKey.publicKey())
            signSemaphore.signal()
            return (signedData.base64urlEncodedString(), publicKey.base64urlEncodedString(), nonceBase64)
        }
        signSemaphore.signal()
        return nil
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
}
