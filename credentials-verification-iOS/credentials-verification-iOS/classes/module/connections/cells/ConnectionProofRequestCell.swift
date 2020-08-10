//
//  ConnectionProofRequestCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 01/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

protocol ConnectionProofRequestCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ConnectionProofRequestCell)
    func tappedAction(for cell: ConnectionProofRequestCell)
}

class ConnectionProofRequestCell: BaseTableViewCell, SwitchCustomDelegate {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var checkbox: SwitchCustomView!

    var credential: Credential!

    override class func default_NibName() -> String {
        return "ConnectionProofRequestCell"
    }

    var delegateImpl: ConnectionProofRequestCellPresenterDelegate? {
        return delegate as? ConnectionProofRequestCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Component delegates

   func stateChanged(for view: SwitchCustomView, newState: Bool) {
        self.delegateImpl?.tappedAction(for: self)
    }

    // MARK: Config

    func config(credential: Credential) {
        self.checkbox.changeState(newState: false)
        checkbox.delegate = self
        self.credential = credential
        switch CredentialType(rawValue: credential.type) {
        case .governmentIssuedId:
            labelTitle.text = "credentials_detail_title_type_government_id".localize()
        case .univerityDegree:
            labelTitle.text = "credentials_detail_title_type_university".localize()
        case .proofOfEmployment:
            labelTitle.text = "credentials_detail_title_type_employment".localize()
        case .certificatOfInsurance:
            labelTitle.text = "credentials_detail_title_type_insurance".localize()
        case .none:
            print("Undefined credential type")
        }

    }
}
