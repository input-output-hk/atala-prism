//
//  ProfileDetailViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 24/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

class ProfileDetailViewController: ListingBaseViewController, UIImagePickerControllerDelegate,
                                 UINavigationControllerDelegate {
    
    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    
    var presenterImpl = ProfileDetailPresenter()
    override var presenter: BasePresenter { return presenterImpl }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let title: String = "profile_detail_title".localize()
        
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: title, hasBackButton: true)

        // Navigation bar
        NavBarCustom.config(view: self)
    }
    
    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .header:
            return "header"
        case .field:
            return "detail"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .header:
            return ProfileDetailHeaderCell.default_NibName()
        case .field:
            return ProfileDetailCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: TextField

    override func getScrollableMainView() -> UIScrollView? {
        return table
    }
}
