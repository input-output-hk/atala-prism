//
//  SignupController.swift
//  credentials-verification-iOS
//
//  Created by vanina on 16/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class WelcomeViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // - background
        let background = UIView()
        background.backgroundColor = .white
        view.addSubview(background)
        background.fitScreen()

        // - logo
        // -- logo area
        let topCenteredView = UIView()
        background.addSubview(topCenteredView)

        topCenteredView.anchor(
            top: background.safeAreaLayoutGuide.topAnchor,
            bottom: background.centerYAnchor,
            leading: background.leadingAnchor,
            trailing: background.trailingAnchor
        )

        // -- logo
        let atalaBlackLogo = UIImageView()
        atalaBlackLogo.image = UIImage(named: "atala_black_title")
        topCenteredView.addSubview(atalaBlackLogo)

        atalaBlackLogo.translatesAutoresizingMaskIntoConstraints = false
        atalaBlackLogo.centerXAnchor.constraint(equalTo: topCenteredView.centerXAnchor).isActive = true
        atalaBlackLogo.centerYAnchor.constraint(equalTo: topCenteredView.centerYAnchor).isActive = true
        atalaBlackLogo.contentMode = .scaleAspectFit

        // -- line
        let line = UIImageView()
        line.image = UIImage(named: "line")
        topCenteredView.addSubview(line)

        line.translatesAutoresizingMaskIntoConstraints = false
        line.centerXAnchor.constraint(equalTo: topCenteredView.centerXAnchor).isActive = true
        line.topAnchor.constraint(equalTo: atalaBlackLogo.bottomAnchor, constant: 12).isActive = true
        line.contentMode = .scaleAspectFit

        // -- welcome label
        let welcomeLabel = UILabel()
        welcomeLabel.text = "welcome" // TODO: - i18n
        topCenteredView.addSubview(welcomeLabel)
        welcomeLabel.textColor = .black
        welcomeLabel.font = UIFont(name: "SFProDisplay-Semibold", size: 22)

        welcomeLabel.translatesAutoresizingMaskIntoConstraints = false
        welcomeLabel.centerXAnchor.constraint(equalTo: topCenteredView.centerXAnchor).isActive = true
        welcomeLabel.topAnchor.constraint(equalTo: line.bottomAnchor, constant: 12).isActive = true
        welcomeLabel.contentMode = .scaleAspectFit

        // - Button
        // -- big shadow
        let redThing = GenericRedButtonView()
        background.addSubview(redThing)
        redThing.bottomAnchor.constraint(equalTo: background.safeAreaLayoutGuide.bottomAnchor, constant: -12).isActive = true
        redThing.centerXAnchor.constraint(equalTo: background.centerXAnchor).isActive = true
        redThing.leadingAnchor.constraint(equalTo: background.leadingAnchor, constant: 30).isActive = true
        redThing.heightAnchor.constraint(equalToConstant: 45).isActive = true

        // Do any additional setup after loading the view.
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
