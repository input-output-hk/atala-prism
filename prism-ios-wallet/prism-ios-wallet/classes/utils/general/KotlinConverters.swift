//
//  KotlinConverters.swift
//  credentials-verification-iOS
//
//  Created by Daniyar Itegulov on 7/1/21.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import crypto

func toKotlinBytes(array: [Int8]) -> KotlinByteArray {
    let result = KotlinByteArray(size: Int32(array.count))
    for index in array.indices {
        result.set(index: Int32(index), value: array[index])
    }
    return result
}

func toKotlinBytes(data: Data) -> KotlinByteArray {
    let result = KotlinByteArray(size: Int32(data.count))
    for index in data.indices {
        result.set(index: Int32(index), value: Int8(bitPattern: data[index]))
    }
    return result
}

func fromKotlinBytes(bytes: KotlinByteArray) -> Data {
    var data = Data(count: Int(bytes.size))
    for index in data.indices {
        data[index] = UInt8(bitPattern: bytes.get(index: Int32(index)))
    }
    return data
}
