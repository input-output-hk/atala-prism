//
//  ViewController.swift
//  credentials-verification-iOS
//
//  Created by Federico on 07/10/2019.
//  Copyright © 2019 iohk. All rights reserved.
//

import UIKit

class ViewController: UIViewController {

    @IBOutlet weak var issuerDID: UILabel!
    @IBOutlet weak var issuerLaber: UILabel!

    lazy var simpleConnection = {
        SimpleConnection()
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
    }

    var counter = 0

    @IBAction func onClick(_ sender: UIButton) {
        counter = counter + 1
        do {
            let issuer = try simpleConnection.getConnection(token: "FakeToken\(counter)").issuer
            issuerLaber.text = issuer.name
            issuerDID.text = issuer.did
        } catch {
            issuerLaber.text = "connection error"
            issuerDID.text = "connection error"
        }
    }
}
