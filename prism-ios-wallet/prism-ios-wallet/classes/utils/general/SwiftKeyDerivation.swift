//
//  SwiftKeyDerivation.swift
//  credentials-verification-iOS
//
//  Created by Daniyar Itegulov on 7/1/21.
//  Copyright © 2021 iohk. All rights reserved.
//

import BitcoinKit
import Foundation
import crypto

class SwiftKeyDerivation: KeyDerivation {
    public static let global = SwiftKeyDerivation()

    private static let mnemonicCodes = MnemonicCodeEnglish()

    func getValidMnemonicWords() -> [String] {
        return SwiftKeyDerivation.mnemonicCodes.wordList
    }

    func isValidMnemonicWord(word: String) -> Bool {
        return SwiftKeyDerivation.mnemonicCodes.wordList.contains(word)
    }

    func randomMnemonicCode() -> MnemonicCode {
        let mnemonics = try? Mnemonic.generate()
        let usedMnemonics = Array(mnemonics![0 ..< CryptoUtils.SEED_COUNT])

        return MnemonicCode(words: usedMnemonics)
    }

    func binarySeed(seed: MnemonicCode, passphrase: String) -> [KotlinByte] {
        do {
            return try toKotlinBytes(data: Mnemonic.seed(mnemonic: seed.words, passphrase: passphrase))
        } catch {
            Logger.e(error.localizedDescription)
            fatalError()
        }
    }

    func derivationRoot(seed: [KotlinByte]) -> ExtendedKey {
        let key = HDPrivateKey(seed: Data(seed.map { $0.uint8Value }), network: Network.mainnetBTC)
        return SwiftExtendedKey(key: key)
    }

    func deriveKey(seed: [KotlinByte], path: DerivationPath) -> ExtendedKey {
        return path.axes.reduce(derivationRoot(seed: seed), { key, axis in key.derive(axis: axis)})
    }
}
