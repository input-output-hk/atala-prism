//
//  MnemonicsTests.swift
//  credentials-verification-iOSTests
//
//  Created by Leandro Pardo on 04/02/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import XCTest
//@testable import ATALA
import prism_kotlin_sdk
import BitcoinKit

class MnemonicsTests: XCTestCase {

    override func setUp() {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

//    func testKeyGeneration() {
//        // The data used for this test was generated at https://iancoleman.io/bip39/#english
//        let crypto = CryptoUtils.global
//
//        crypto.usedMnemonics = ["soul", "satoshi", "birth", "under", "sweet", "spot", "away", "there", "canoe", "crack", "unknown", "maid"]
//        crypto.generateSeed()
//        XCTAssertEqual(crypto.seed?.hex, "5eab45aaaeafe9dbe716071f36bd754132e5deda91dd58a509b61943e88f91153c9e5d9a272d4f909504b96f46509427135e8d2feda2578a2ed1071d0928ba5f")
//        crypto.generatePrivateKey()
//        XCTAssertEqual(crypto.pk?.extended(), "xprv9s21ZrQH143K3XkgFtA4W8mSZtVGCz61muiEWQz6XwaQ6ZPkPvX9DFEwMKr5VB6zEHbzsMRPDdQVHeVA3wkBTSxW6dqWgf2czCqrywKTnQe")
//
//        crypto.usedMnemonics = ["teach", "daring", "onion", "thank", "reform", "measure", "rhythm", "urge", "symptom", "maze", "siege", "sheriff"]
//        crypto.generateSeed()
//        XCTAssertEqual(crypto.seed?.hex, "18b02d341200e73c2c5f37f1690912bd3652d4fc9c500e7552405d8f54cf72451a4486b6b76084879e465c56060e097a7b8f5ed3037e612524fbb719e1d3c3cf")
//        crypto.generatePrivateKey()
//        XCTAssertEqual(crypto.pk?.extended(), "xprv9s21ZrQH143K34t6ULQXVYuJ7TDF83NeXQfYtzKhrBB2BU5YBB3yHKTK5n6HzNrnS4XzJ6HxgXmgwm5CkioBgMsrKLdyeoJ3MT7hoeAu4V4")
//    }

    func testSHA256() {
        let digest = SHA256Digest.Companion().compute(bytes: [0, 1, 2])
        XCTAssertEqual(digest.value.count, Int(SHA256Digest.Companion().BYTE_LENGTH))
        XCTAssertEqual(digest.hexValue().count, Int(SHA256Digest.Companion().HEX_STRING_LENGTH))
        XCTAssertEqual(digest.hexValue(), "ae4b3280e56e2faf83f414a6e3dabe9d5fbe18976544c05fed121accb85b53fc")
    }

    func testEC() {
        let keyPair = EC.init().generateKeyPair()
        XCTAssertEqual(keyPair.privateKey.getHexEncoded().count, 64)
        XCTAssertEqual(keyPair.publicKey.getHexEncoded().count, 130)
    }

    class SwiftKeyDerivation: KeyDerivation {
        func randomMnemonicCode() -> MnemonicCode {
            let words = (try? Mnemonic.generate())!

            return MnemonicCode(words: words)
        }
    }

    func testSeedGeneration() {
        let binarySeedGenerator = BinarySeedGenerator(keyDerivation: SwiftKeyDerivation())
        let seed = binarySeedGenerator.randomBinarySeed(passphrase: "12345")
        XCTAssertEqual(seed.count, 64)
    }
}
