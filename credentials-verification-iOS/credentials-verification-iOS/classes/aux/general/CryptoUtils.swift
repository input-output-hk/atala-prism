//
import BitcoinKit

class CryptoUtils: NSObject {

    static let global = CryptoUtils()

    static let SEED_COUNT = 12

    var mnemonics: [String]?
    var usedMnemonics: [String]?
    var seed: Data?

    func setupMnemonics() {

        mnemonics = try! Mnemonic.generate()
        usedMnemonics = Array(mnemonics![0 ..< CryptoUtils.SEED_COUNT])

        let passphrase = ""
        let mnemonic = usedMnemonics!.joined(separator: " ").decomposedStringWithCompatibilityMapping.data(using: .utf8)!
        let salt = ("mnemonic" + passphrase).decomposedStringWithCompatibilityMapping.data(using: .utf8)!
        seed = _Key.deriveKey(mnemonic, salt: salt, iterations: 2048, keyLength: 64)
    }

    func getUsedRandomIndexes(count: Int) -> [Int] {
        let indexes = Array(0 ..< CryptoUtils.SEED_COUNT)
        return Array(indexes.randomPick(count))
    }

    func checkWordsValidity(indexes: [Int], words: [String]) -> Bool {
        for i in 0 ..< indexes.count {
            if usedMnemonics![indexes[i]] != words[i] {
                return false
            }
        }
        return true
    }
}
