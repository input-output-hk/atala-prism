//
//  ButtonViewController.swift
//  credentials-verification-iOS
//
//  Created by vanina on 17/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class ButtonViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Rectangle

        let view = UILabel()

        view.frame = CGRect(x: 0, y: 0, width: 315, height: 45)

        view.backgroundColor = .white

        self.view = view

        let shadows = UIView()

        shadows.frame = view.frame

        shadows.clipsToBounds = false

        view.addSubview(shadows)

        let shadowPath0 = UIBezierPath(roundedRect: shadows.bounds, cornerRadius: 50)

        let layer0 = CALayer()

        layer0.shadowPath = shadowPath0.cgPath

        layer0.shadowColor = UIColor(red: 1, green: 0.18, blue: 0.23, alpha: 1).cgColor

        layer0.shadowOpacity = 1

        layer0.shadowRadius = 6

        layer0.shadowOffset = CGSize(width: 0, height: 0)

        layer0.bounds = shadows.bounds

        layer0.position = shadows.center

        shadows.layer.addSublayer(layer0)

        let shapes = UIView()

        shapes.frame = view.frame

        shapes.clipsToBounds = true

        view.addSubview(shapes)

        let layer1 = CALayer()

        layer1.backgroundColor = UIColor(red: 1, green: 0.18, blue: 0.23, alpha: 1).cgColor

        layer1.bounds = shapes.bounds

        layer1.position = shapes.center

        shapes.layer.addSublayer(layer1)

        shapes.layer.cornerRadius = 50

        let parent = self.view!

        parent.addSubview(view)

        view.translatesAutoresizingMaskIntoConstraints = false

        view.widthAnchor.constraint(equalToConstant: 315).isActive = true

        view.heightAnchor.constraint(equalToConstant: 45).isActive = true

        view.leadingAnchor.constraint(equalTo: parent.leadingAnchor, constant: 30).isActive = true

        view.topAnchor.constraint(equalTo: parent.topAnchor, constant: 737).isActive = true

        // Do any additional setup after loading the view.
    }
}
