//
//  SearchBarCustom.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 24/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class SearchBarCustom: UIView {

    var searchBar: UISearchBar!

    override func awakeFromNib() {
        // the actual search barw
        self.searchBar = UISearchBar(frame: self.frame)

        self.searchBar.clipsToBounds = true

        // the smaller the number in relation to the view, the more subtle
        // the rounding -- https://www.hackingwithswift.com/example-code/calayer/how-to-round-the-corners-of-a-uiview
        self.searchBar.layer.cornerRadius = 5

        self.addSubview(self.searchBar)

        self.searchBar.translatesAutoresizingMaskIntoConstraints = false

        let leadingConstraint = NSLayoutConstraint(item: self.searchBar, attribute: .leading,
                                                   relatedBy: .equal, toItem: self, attribute: .leading,
                                                   multiplier: 1, constant: 20)
        let trailingConstraint = NSLayoutConstraint(item: self.searchBar, attribute: .trailing,
                                                    relatedBy: .equal, toItem: self, attribute: .trailing,
                                                    multiplier: 1, constant: -20)
        let yConstraint = NSLayoutConstraint(item: self.searchBar, attribute: .centerY,
                                             relatedBy: .equal, toItem: self, attribute: .centerY,
                                             multiplier: 1, constant: 0)

        self.addConstraints([yConstraint, leadingConstraint, trailingConstraint])

        self.searchBar.backgroundColor = .appWhite
        self.searchBar.setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
        self.searchBar.tintColor = .appWhite
        self.searchBar.isTranslucent = true

        // https://stackoverflow.com/questions/21191801/how-to-add-a-1-pixel-gray-border-around-a-uisearchbar-textfield/21192270
        searchBar.searchTextField.layer.borderWidth = 1.0
        searchBar.searchTextField.layer.cornerRadius = 6
        searchBar.searchTextField.layer.borderColor = UIColor.appGreyMid.cgColor
        searchBar.searchTextField.backgroundColor = .appWhite
    }

}
