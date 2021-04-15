//
//  CredentialVerificationDetailViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 12/10/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import Presentr

class CredentialVerificationDetailViewController: UIViewController {

    @IBOutlet weak var mainView: UIView!
    @IBOutlet weak var verifiedView: UIView!
    @IBOutlet weak var originalView: UIView!
    @IBOutlet weak var originalIconBg: UIView!
    @IBOutlet weak var fileHashView: UIView!
    @IBOutlet weak var fileHashIconBg: UIView!
    @IBOutlet weak var fileHashLbl: UILabel!
    @IBOutlet weak var txHashView: UIView!
    @IBOutlet weak var txHashIconBg: UIView!
    @IBOutlet weak var txHashLbl: UILabel!
    @IBOutlet weak var dateView: UIView!
    @IBOutlet weak var dateLbl: UILabel!
    @IBOutlet weak var fileView: UIView!
    @IBOutlet weak var fileTv: UITextView!
    @IBOutlet weak var closeFileBttn: UIButton!
    @IBOutlet weak var downloadFileBttn: UIButton!

    var credential: Credential?

    override func viewDidLoad() {
        super.viewDidLoad()

//        // Do any additional setup after loading the view.
        mainView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, onlyTops: true)
        fileView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, onlyTops: true)
        closeFileBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        downloadFileBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        verifiedView.addRoundCorners(radius: 4)
        dateView.addRoundCorners(radius: 4)
        originalView.addRoundCorners(radius: 6)
        originalView.addDropShadow(radius: 20, opacity: 0.05, offset: CGSize(width: 0, height: 0), color: .appBlack)
        originalIconBg.addRoundCorners(radius: 8)
        fileHashView.addRoundCorners(radius: 6)
        originalIconBg.addRoundCorners(radius: 8)
        fileHashIconBg.addRoundCorners(radius: 6)
        txHashIconBg.addRoundCorners(radius: 8)
        fileTv.addRoundCorners(radius: 10)

        setData()
    }

    static func makeThisView() -> CredentialVerificationDetailViewController {
        let storyboard = UIStoryboard(name: "CredentialVerificationDetail", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "CredentialVerificationDetail")
            as? CredentialVerificationDetailViewController {
            return viewcontroller
        }
        return CredentialVerificationDetailViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .bottomHalf)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        presenter.presentationType = .fullScreen
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(credential: Credential?) {
        self.credential = credential
    }

    func setData() {

        txHashLbl.text = "#a4b412fdf47dfd457djhgf3bftjtyhn6hw45hwhw45gg345"
        dateLbl.text = "credentials_verify_date_time".localize()
            .appending(credential?.dateReceived.dateTimeString() ?? "")

        fileTv.text = credential?.json

        if let plainJsonData = credential?.json.data(using: .utf8) {
            let digest = CryptoUtils.sha256Digest.compute(bytes: toKotlinBytes(data: plainJsonData))
            fileHashLbl.text = "#\(digest.hexValue())"
        }
    }

    func saveFile() {
        showLoading(doShow: true)
        do {
            let documentsUrl = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask,
                                                           appropriateFor: nil, create: true)
            var fileName: String = "\(credential?.credentialName ?? "")_\(credential?.credentialId ?? "")"
            var fileToSave = ""
            if let credentialStr = credential?.plainCredential {
                fileToSave = credentialStr
                fileName.append(".txt")
            } else {
                fileToSave = fileTv.text
                fileName.append(".json")
            }
            // Destination file url
            let destination = documentsUrl.appendingPathComponent(fileName)
            try fileToSave.write(to: destination, atomically: true, encoding: .utf8)
            let successMsg = "\("credentials_verify_download_success".localize() ?? "")\n\(fileName)"
            showSuccessMessage(doShow: true, message: successMsg)
        } catch {
            showErrorMessage(doShow: true, message: "credentials_verify_download_error".localize())
        }
        showLoading(doShow: false)
    }

    // MARK: ButtonActions

    @IBAction func showFileTapped(_ sender: Any) {
        fileView.isHidden = false
    }

    @IBAction func copyFileHashTapped(_ sender: Any) {
        UIPasteboard.general.string = fileHashLbl.text
    }

    @IBAction func copyTxHashTapped(_ sender: Any) {
        UIPasteboard.general.string = txHashLbl.text
    }

    @IBAction func closeFileTapped(_ sender: Any) {
        fileView.isHidden = true
    }

    @IBAction func downloadFileTapped(_ sender: Any) {
        self.saveFile()
    }

    @IBAction func dismissTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
}
