//
//  ConnectionProofRequestViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 30/03/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Presentr
import UIKit

protocol ConnectionProofRequestPresenterDelegate: class {

    func tappedDeclineAction(for: ConnectionProofRequestViewController)
    func tappedConfirmAction(for: ConnectionProofRequestViewController)
}

class ConnectionProofRequestViewController: UIViewController, PresentrDelegate {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var tableCredentials: UITableView!
    @IBOutlet weak var tableHeightCtrt: NSLayoutConstraint!

    weak var delegate: ConnectionProofRequestPresenterDelegate?
    var contact: Contact?
    var credentials: [Credential] = []
    var selectedCredentials: [Credential] = []
    var requiered: [String] = []

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.

        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    static func makeThisView() -> ConnectionProofRequestViewController {
        let storyboard = UIStoryboard(name: "ConnectionProofRequest", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "ConnectionProofRequest")
                                as? ConnectionProofRequestViewController {
            return viewcontroller
        }
        return ConnectionProofRequestViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 367)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(delegate: ConnectionProofRequestPresenterDelegate?, contact: Contact,
                credentials: [Credential], requiered: [String], logoData: Data?, placeholderNamed: String?) {

        self.delegate = delegate
        self.credentials = credentials
        self.contact = contact
        self.requiered = requiered
        self.selectedCredentials.removeAll()
        self.labelTitle.text = contact.name
        self.imageLogo.applyDataImage(data: logoData, placeholderNamed: placeholderNamed)
        if credentials.count > 1 {
            tableHeightCtrt.constant = 80
        }
        buttonConfirm.isEnabled = false
        buttonConfirm.backgroundColor = .appGreyMid
        tableCredentials.reloadData()
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.delegate?.tappedConfirmAction(for: self)
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.delegate?.tappedDeclineAction(for: self)
    }

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        self.delegate?.tappedDeclineAction(for: self)
        return false
    }

}

extension ConnectionProofRequestViewController: UITableViewDataSource, ConnectionProofRequestCellPresenterDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return credentials.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "ConnectionProofRequestCell")
            as? ConnectionProofRequestCell else {
            return UITableViewCell()
        }
        cell.config(credential: credentials[indexPath.row])
        cell.delegate = self
        return cell
    }

    func setup(for cell: ConnectionProofRequestCell) {

    }

    func tappedAction(for cell: ConnectionProofRequestCell) {
        if cell.checkbox.getState() {
            selectedCredentials.append(cell.credential)
        } else {
            selectedCredentials.remove(cell.credential)
        }
        var isComplete = true
        for type in requiered {
            isComplete = isComplete && selectedCredentials.contains {
                $0.type == type
            }
        }

        buttonConfirm.isEnabled = isComplete
        buttonConfirm.backgroundColor = isComplete ? .appRed : .appGreyMid
    }

}
