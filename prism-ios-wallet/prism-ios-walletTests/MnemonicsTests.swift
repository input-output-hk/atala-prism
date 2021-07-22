//
//  MnemonicsTests.swift
//  credentials-verification-iOSTests
//
//  Created by Leandro Pardo on 04/02/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import XCTest
@testable import Atala_PRISM
import Crypto
import BitcoinKit

class MnemonicsTests: XCTestCase {

    override func setUp() {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

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

    func testKeyGeneration() {
        let keyDerivation = SwiftKeyDerivation.global

        let mnemonicCode1 = MnemonicCode(
            words: ["soul", "satoshi", "birth", "under", "sweet", "spot",
                    "away", "there", "canoe", "crack", "unknown", "maid"]
        )
        let seed1 = keyDerivation.binarySeed(seed: mnemonicCode1, passphrase: "")
        XCTAssertEqual(
            fromKotlinBytes(bytes: seed1).hex,
            "5eab45aaaeafe9dbe716071f36bd754132e5deda91dd58a509b61943e88f91153c9" +
                "e5d9a272d4f909504b96f46509427135e8d2feda2578a2ed1071d0928ba5f"
        )
        let key1 = keyDerivation.derivationRoot(seed: seed1)
        XCTAssertEqual(
            key1.publicKey().getHexEncoded(),
            "04687b90198803de4512905b7f31433d7e83020cd223700a88e14027ef8058e4c9f4" +
                "f3e2629e10f983b7c808cb84282a11a8ebb9eec709759907ec7d84e09ef34a"
        )

        let mnemonicCode2 = MnemonicCode(
            words: ["teach", "daring", "onion", "thank", "reform", "measure",
                    "rhythm", "urge", "symptom", "maze", "siege", "sheriff"]
        )
        let seed2 = keyDerivation.binarySeed(seed: mnemonicCode2, passphrase: "")
        XCTAssertEqual(
            fromKotlinBytes(bytes: seed2).hex,
            "18b02d341200e73c2c5f37f1690912bd3652d4fc9c500e7552405d8f54cf72451a44" +
                "86b6b76084879e465c56060e097a7b8f5ed3037e612524fbb719e1d3c3cf"
        )
        let key2 = keyDerivation.derivationRoot(seed: seed2)
        XCTAssertEqual(
            key2.publicKey().getHexEncoded(),
            "04ce6d2ec683b1d96689229753d25a9f931a0625278115770ea230e7074196b15053" +
                "a84ce40f9b890803cca0cb07b1368a1235ecc3dd3100f6b2927ac0098b9182"
        )
    }

    func testKeyDerivation() {
        let keyDerivation = SwiftKeyDerivation.global
        let mnemonicCode = keyDerivation.randomMnemonicCode()
        let seed = keyDerivation.binarySeed(seed: mnemonicCode, passphrase: "")
        let key = keyDerivation.deriveKey(
            seed: seed,
            path: DerivationPath.Companion.init().fromPath(path: "m/0/1/2")
        )
        let path = key.path().axes.map { $0.number }
        XCTAssertEqual(path, [0, 1, 2])
    }
}
