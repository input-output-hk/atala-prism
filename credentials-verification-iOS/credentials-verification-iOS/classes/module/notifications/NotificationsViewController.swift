//
//  NotificationsViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 16/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class NotificationsViewController: ListingBaseViewController {

    var presenterImpl = NotificationsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    // Views
    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewScanQr: UIView!
    @IBOutlet weak var viewTable: UIView!
    // Scan QR
    @IBOutlet weak var viewQrScannerContainer: UIView!
    let scanner = QRCode()

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupEmptyView()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.actionPullToRefresh()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        // Setup (views thar require others to be resized first)
        setupQrScanner()
    }

    @discardableResult
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    func setupEmptyView() {

        viewEmpty.config(imageNamed: "img_qr_red", title: "notifications_empty_title".localize(),
                         subtitle: "notifications_empty_subtitle".localize(),
                         buttonText: "connections_empty_button".localize(), buttonAction: actionScan)
    }

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let credentialsMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing
        let isScanningQr = presenterImpl.isScanningQr()

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewScanQr.isHidden = !isScanningQr
        viewTable.isHidden = isEmpty || isScanningQr

        // Change the nav bar
        if credentialsMode == .detail {
            let detailDegree = presenterImpl.detailDegree
            var navTitle = ""
            switch detailDegree!.type {
            case .univerityDegree:
                navTitle = "credentials_detail_title_type_university".localize()
            case .governmentIssuedId:
                navTitle = "credentials_detail_title_type_government_id".localize()
            case .certificatOfInsurance:
                navTitle = "credentials_detail_title_type_insurance".localize()
            case .proofOfEmployment:
                navTitle = "credentials_detail_title_type_employment".localize()
            default:
                print("Unrecognized type")
            }
            navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle,
                                       hasBackButton: credentialsMode != .degrees, rightIconName: nil,
                                       rightIconAction: nil)
        } else {
            let navTitle = isScanningQr
                ? "notifications_scan_qr_nav_title".localize()
                : "notifications_title".localize()
            let navIconName = (!isEmpty && !isScanningQr && mode != .fetching) ? "ico_qr" : nil
            navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle,
                                       hasBackButton: isScanningQr, rightIconName: navIconName,
                                       rightIconAction: actionScan)
        }

        NavBarCustom.config(view: self)
    }

    func config(isLoading: Bool) {
        showLoading(doShow: isLoading)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .degree:
            return "common"
        case .newDegreeHeader:
            return "newDegreeHeader"
        case .newDegree:
            return "newDegree"
        case .document:
            return "document"
        case .detailHeader:
            return "detailHeader"
        case .detailProperty:
            return "detailProperty"
        case .detailFooter:
            return "detailFooter"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .degree:
            return DegreeViewCell.default_NibName()
        case .newDegreeHeader:
            return NewDegreeHeaderViewCell.default_NibName()
        case .newDegree:
            return NewDegreeViewCell.default_NibName()
        case .document:
            return DocumentViewCell.default_NibName()
        case .detailHeader:
            return DetailHeaderViewCell.default_NibName()
        case .detailProperty:
            return DetailPropertyViewCell.default_NibName()
        case .detailFooter:
            return DetailFooterViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    lazy var actionScan = SelectorAction(action: { [weak self] in
          self?.presenterImpl.tappedScanButton()
    })

    // MARK: Scan QR

    func setupQrScanner() {

        scanner.prepareScan(viewQrScannerContainer) { (stringValue) -> Void in
            self.presenterImpl.scannedQrCode(stringValue)
        }
        scanner.scanFrame = viewQrScannerContainer.frame
        scanner.autoRemoveSubLayers = true
        scanner.lineWidth = 0
        scanner.strokeColor = UIColor.appRed
        scanner.maxDetectedCount = 1
    }

    func startQrScan() {

        scanner.clearDrawLayer()
        scanner.startScan()
    }

    func stopQrScan() {
        scanner.stopScan()
    }

    func showNewConnectMessage(type: Int, title: String?, logoData: Data?) {

        let confirmMessage = ConnectionConfirmViewController.makeThisView()

        customPresentViewController(confirmMessage.presentr, viewController: confirmMessage, animated: true)
        confirmMessage.config(delegate: presenterImpl, lead: "connections_scan_qr_confirm_title".localize(),
                              title: title, logoData: logoData, placeholderNamed: "ico_placeholder_credential")
    }
}
