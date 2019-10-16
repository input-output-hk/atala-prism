//
//  SimpleConnection.swift
//  credentials-verification-iOS
//
//  Created by vanina on 08/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class SimpleConnection: NSObject {
    lazy var service: Connector_ConnectorUserServiceServiceClient = {
        return Connector_ConnectorUserServiceServiceClient(address: "localhost:50051", secure: false)
    }()
    
    func getConnection(token: String) throws -> Connector_GetConnectionTokenInfoResponse {
        return try service.getConnectionTokenInfo(Connector_GetConnectionTokenInfoRequest.with {
            $0.token = token
        })
    }
}
