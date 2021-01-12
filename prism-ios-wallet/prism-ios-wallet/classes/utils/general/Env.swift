//
//  Env.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 04/06/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Foundation

struct Env {

    private static let production: Bool = {
        #if debug
            return false
        #else
            return true
        #endif
    }()

    static func isProduction () -> Bool {
        return self.production
    }

}
