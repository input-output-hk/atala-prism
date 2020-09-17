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
    @IBOutlet weak var viewTable: UIView!
    @IBOutlet weak var viewDetail: CredentialDetailView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    lazy var actionHistory = SelectorAction(action: { [weak self] in
          self?.presenterImpl.tappedHistoryButton()
    })

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupEmptyView()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.mode = .degrees
        self.presenterImpl.actionPullToRefresh()
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

        viewEmpty.config(imageNamed: "img_notifications_tray", title: "notifications_empty_title".localize(),
                         subtitle: "notifications_empty_subtitle".localize(),
                         buttonText: nil, buttonAction: nil)
    }

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let credentialsMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing
        let isDetail = credentialsMode == .detail

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty || isDetail
        viewDetail.isHidden = !isDetail

        // Change the nav bar

        if isDetail, let detailCredential = presenterImpl.detailCredential {
            let navTitle = detailCredential.credentialName
            viewDetail.config(credential: detailCredential)
            navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle,
                                       hasBackButton: credentialsMode != .degrees, rightIconName: nil,
                                       rightIconAction: nil)
        } else if credentialsMode == .activityLog {
            navBar = NavBarCustomStyle(hasNavBar: true, title: "activitylog_title".localize(),
                                       hasBackButton: true, rightIconName: nil, rightIconAction: nil)
        } else {
            let navTitle =  "notifications_title".localize()
            let navIconName = mode != .fetching ? "ico_history" : nil
            navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle, hasBackButton: false,
                                       rightIconName: navIconName, rightIconAction: actionHistory)
            viewDetail.clearWebView()
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
            return "notificationHeader"
        case .newDegree:
            return "notification"
        case .activityLog:
            return "activityLog"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .degree:
            return DegreeViewCell.default_NibName()
        case .newDegreeHeader:
            return NotificationHeaderViewCell.default_NibName()
        case .newDegree:
            return NotificationViewCell.default_NibName()
        case .activityLog:
            return ActivityLogTableViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

}
