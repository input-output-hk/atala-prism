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

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

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
        } else if credentialsMode == .activityLog {
            navBar = NavBarCustomStyle(hasNavBar: true, title: "activitylog_title".localize(),
                                       hasBackButton: true, rightIconName: nil, rightIconAction: nil)
        } else {
            let navTitle =  "notifications_title".localize()
            let navIconName = mode != .fetching ? "ico_history" : nil
            navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle, hasBackButton: false,
                                       rightIconName: navIconName, rightIconAction: actionHistory)
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
        case .document:
            return "document"
        case .detailHeader:
            return "detailHeader"
        case .detailProperty:
            return "detailProperty"
        case .detailFooter:
            return "detailFooter"
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
        case .document:
            return DocumentViewCell.default_NibName()
        case .detailHeader:
            return DetailHeaderViewCell.default_NibName()
        case .detailProperty:
            return DetailPropertyViewCell.default_NibName()
        case .detailFooter:
            return DetailFooterViewCell.default_NibName()
        case .activityLog:
            return ActivityLogTableViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

}
