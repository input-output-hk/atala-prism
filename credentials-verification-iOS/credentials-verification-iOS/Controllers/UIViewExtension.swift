//
//  SignupController.swift
//  credentials-verification-iOS
//
//  Created by vanina on 16/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

extension UIView {

    func anchor(
        top: NSLayoutYAxisAnchor,
        bottom: NSLayoutYAxisAnchor,
        leading: NSLayoutXAxisAnchor,
        trailing: NSLayoutXAxisAnchor
    ) {
        translatesAutoresizingMaskIntoConstraints = false

        topAnchor.constraint(equalTo: top).isActive = true
        bottomAnchor.constraint(equalTo: bottom).isActive = true
        leadingAnchor.constraint(equalTo: leading).isActive = true
        trailingAnchor.constraint(equalTo: trailing).isActive = true
    }

    func fitSafeArea() {
        if let parentView = superview {
            self.anchor(
                top: parentView.safeAreaLayoutGuide.topAnchor,
                bottom: parentView.safeAreaLayoutGuide.bottomAnchor,
                leading: parentView.leadingAnchor,
                trailing: parentView.trailingAnchor
            )
        }
    }

    func fitScreen() {
        if let parentView = superview {
            self.anchor(
                top: parentView.topAnchor,
                bottom: parentView.bottomAnchor,
                leading: parentView.leadingAnchor,
                trailing: parentView.trailingAnchor
            )
        }
    }
}
