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
        background.backgroundColor = .black
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
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
