//
//  SwiftExtendedKey.swift
//  credentials-verification-iOS
//
//  Created by Daniyar Itegulov on 7/1/21.
//  Copyright © 2021 iohk. All rights reserved.
//

import BitcoinKit
import Foundation
import crypto

class SwiftExtendedKey: ExtendedKey {
    private static let globalEc = EC()

    private let key: HDPrivateKey
    private let derivationPath: DerivationPath

    private init(key: HDPrivateKey, derivationPath: DerivationPath) {
        self.key = key
        self.derivationPath = derivationPath
    }

    init(key: HDPrivateKey) {
        self.key = key
        self.derivationPath = DerivationPath(axes: [])
    }

    func derive(axis: DerivationAxis) -> ExtendedKey {
        do {
            let newKey = try key.derived(at: UInt32(axis.number), hardened: axis.hardened)
            return SwiftExtendedKey(key: newKey, derivationPath: derivationPath.derive(axis: axis))
        } catch {
            Logger.e(error.localizedDescription)
            fatalError()
        }
    }

    func keyPair() -> ECKeyPair {
        return ECKeyPair(publicKey: publicKey(), privateKey: privateKey())
    }

    func path() -> DerivationPath {
        return derivationPath
    }

    func privateKey() -> ECPrivateKey {
        return SwiftExtendedKey.globalEc.toPrivateKey(encoded: toKotlinBytes(data: key.privateKey().data))
    }

    func publicKey() -> ECPublicKey {
        return SwiftExtendedKey.globalEc.toPublicKeyFromPrivateKey(privateKey: privateKey())
    }
}
