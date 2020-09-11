//
//  NoResultsViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 25/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Foundation

class NoResultsViewCell: BaseTableViewCell {

    class var reuseIdentifier: String {
        return "noResults"
    }

    override class func default_NibName() -> String {
        return "NoResultsViewCell"
    }

}
