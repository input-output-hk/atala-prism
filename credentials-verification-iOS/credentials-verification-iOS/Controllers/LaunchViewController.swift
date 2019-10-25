//
//  SignupController.swift
//  credentials-verification-iOS
//
//  Created by vanina on 16/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class LaunchViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // - background
        let background = UIView()
        if let backgroundColor = UIColor.init(named: "atala_splash_backgroud_color") {
            background.backgroundColor = backgroundColor
        }
        view.addSubview(background)
        background.fitScreen()
            
        // - splash
        let atalaSplash = UIImageView()
        atalaSplash.image = UIImage(named: "atala_splash")
        background.addSubview(atalaSplash)
        atalaSplash.fitSafeArea()

        // - logo
        // -- area
        let topCenteredLoadingView = UIView()
        atalaSplash.addSubview(topCenteredLoadingView)
        
        topCenteredLoadingView.anchor(
            top: atalaSplash.safeAreaLayoutGuide.topAnchor,
            bottom: atalaSplash.centerYAnchor,
            leading: atalaSplash.leadingAnchor,
            trailing: atalaSplash.trailingAnchor
        )

        // -- image
        let atalaLogo = UIImageView()
        atalaLogo.image = UIImage(named: "atala_white_title")
        topCenteredLoadingView.addSubview(atalaLogo)
        
        atalaLogo.translatesAutoresizingMaskIntoConstraints = false
        atalaLogo.centerXAnchor.constraint(equalTo: topCenteredLoadingView.centerXAnchor).isActive = true
        atalaLogo.centerYAnchor.constraint(equalTo: topCenteredLoadingView.centerYAnchor).isActive = true
        atalaLogo.contentMode = .scaleAspectFit
    }
}
