//
//  ButtonViewController.swift
//  credentials-verification-iOS
//
//  Created by vanina on 17/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class GenericRedButtonView: UIButton {

    // #1
    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setupView()
    }

    // #2
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    // #3
    public convenience init() {
        self.init(frame: .zero)
    }

    private func setupView() {
        translatesAutoresizingMaskIntoConstraints = false
        let shapes = UIView() // shapes ?
        shapes.clipsToBounds = true
        addSubview(shapes)

        let layer1 = CALayer()
        layer1.backgroundColor = UIColor(named: "shadow_color")?.cgColor
        layer1.bounds = shapes.bounds
        layer1.position = shapes.center
        shapes.layer.addSublayer(layer1)
        shapes.layer.cornerRadius = 50

        addSubview(shapes)
    }
}
