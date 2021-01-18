//
//  KotlinConverters.swift
//  credentials-verification-iOS
//
//  Created by Daniyar Itegulov on 7/1/21.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import crypto

func toKotlinBytes(data: Data) -> [KotlinByte] {
    var result = [KotlinByte]()
    for index in data.indices {
        result.append(KotlinByte(value: Int8(bitPattern: data[index])))
    }
    return result
}

func fromKotlinBytes(bytes: [KotlinByte]) -> Data {
    return Data(bytes.map { $0.uint8Value })
}
