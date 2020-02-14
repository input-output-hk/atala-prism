//
import BitcoinKit

class CryptoUtils: NSObject {

    static let global = CryptoUtils()

    static let SEED_COUNT = 12

    var mnemonics: [String]?
    var usedMnemonics: [String]?
    var seed: Data?
    var pk: HDPrivateKey?
    
    func setupMnemonics() {

        mnemonics = try! Mnemonic.generate()
        usedMnemonics = Array(mnemonics![0 ..< CryptoUtils.SEED_COUNT])

        generateSeed()
        generatePrivateKey()
    }

     func generateSeed() {
        let passphrase = ""
        seed = try? Mnemonic.seed(mnemonic: usedMnemonics!, passphrase: passphrase)
    }
    
    func generatePrivateKey() {
        pk = HDPrivateKey(seed: seed!, network: .mainnetBCH)
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
