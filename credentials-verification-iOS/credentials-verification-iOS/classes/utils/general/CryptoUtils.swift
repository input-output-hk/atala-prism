//
import BitcoinKit
import JavaScriptCore
import WebKit
import CommonCrypto

class CryptoUtils: NSObject {

    static let global = CryptoUtils()

    static let SEED_COUNT = 12

    var mnemonics: [String]?
    var usedMnemonics: [String]?
    var seed: Data?
    var lastUsedKeyIndex: Int?

    let signSemaphore = DispatchSemaphore(value: 1)

    override init() {
        super.init()
        self.lastUsedKeyIndex = SharedMemory.global.loggedUser?.lastUsedKeyIndex
        self.usedMnemonics = SharedMemory.global.loggedUser?.mnemonics
        if usedMnemonics != nil {
            self.generateSeed()
        }
    }

    func setupMnemonics() {

        mnemonics = try? Mnemonic.generate()
        usedMnemonics = Array(mnemonics![0 ..< CryptoUtils.SEED_COUNT])

        generateSeed()
    }

     func generateSeed() {
        let passphrase = ""
        do {
            seed = try Mnemonic.seed(mnemonic: usedMnemonics!, passphrase: passphrase)
        } catch {
            Logger.e(error.localizedDescription)
        }
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
        let keychain = HDKeychain(seed: seed!, network: .testnetBTC)
        let derived = try? keychain.derivedKey(path: keyPath)
        let publicKey = derived?.extendedPublicKey().publicKey()
        let pointData = getUncompressedKey(publicKey: publicKey!)
        var protoPublicKey: Io_Iohk_Atala_Prism_Protos_EncodedPublicKey = Io_Iohk_Atala_Prism_Protos_EncodedPublicKey()
        protoPublicKey.publicKey = pointData
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
        let nonceSHA256 = Data(sha256(data: nonceData))
        let keychain = HDKeychain(seed: seed!, network: .testnetBTC)
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

    func sha256(data: Data) -> [UInt8] {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return hash
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
