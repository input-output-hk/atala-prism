//

class ProfileViewController: ListingBaseViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate{

    var presenterImpl = ProfilePresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewTable: UIView!
    @IBOutlet weak var viewBgSpecial: UIView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    var countries: [String] = []
    var picker: PickerViewCell?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onReloadAttributes),
                                               name: .reloadAttributes, object: nil)
    }
    
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }
    
    // MARK: Change tab notifications

    @objc func onReloadAttributes() {
        presenterImpl.startShowingInitial()
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let profileMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        var navTitle = ""
        var iconTitle: String?
        var iconAction: SelectorAction?
        var hasBgSpecial = false

        switch profileMode {
        case .initial:
            hasBgSpecial = true
            navTitle = "profile_nav_title".localize()
            iconTitle = "ico_pencil"
            iconAction = actionEdit
        case .editing:
            hasBgSpecial = true
            navTitle = "profile_edit_nav_title".localize()
            iconTitle = "ico_check"
            iconAction = actionEditSave
        }

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: hasBgSpecial, title: navTitle,
                                   hasBackButton: true, rightIconName: iconTitle,
                                   rightIconAction: iconAction, leftIconName: "ico_add", leftIconAction: addAction)
        
        NavBarCustom.config(view: self)
        // Special background
        viewBgSpecial.isHidden = !hasBgSpecial
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        if section == 0 {
            return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
        }
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR * 3
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .profile:
            return "profile"
        case .initial:
            return "common"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .profile:
            return ProfileViewCell.default_NibName()
        case .initial:
            return CommonViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }
    
    // MARK: Buttons

    func setupButtons() {}

    lazy var actionEdit = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedEditButton()
    })

    lazy var actionEditSave = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedEditSaveButton()
    })
    
    lazy var addAction = SelectorAction(action: { [weak self] in

        let attVC = AttributeViewController.makeThisView()
        
        attVC.config { (type, logo) in
            self?.presenterImpl.addAttributeWith(type: type, logo: logo)
        }
        
        self?.customPresentViewController(attVC.presentr, viewController: attVC, animated: true)
    })
    
    // MARK: Screens

    func changeScreenToProfileDetail() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "ProfileDetailSegue", params: nil)
    }
    
    func changeScreenToAttributeVerification(type: String, logo: String) {
        
        var params: [Any?] = []
        params.append(type)
        params.append(logo)
        
        ViewControllerUtils.changeScreenPresented(caller: self, storyboardName: "BaseVerification", viewControllerIdentif: "BaseVerification", params: params, animated: true)
    }
    
    func changeScreenToAttributeListing(type: String) {
        
        var params: [Any?] = []
        params.append(type)
        
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "AttributeListingSegue", params: params)
    }
    
    // MARK: Profile picture 

    func chooseProfilePicture() {
        let imagePickerController = UIImagePickerController()
        imagePickerController.delegate = self
        imagePickerController.allowsEditing = true
        let actionsheet = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        actionsheet.addAction(UIAlertAction(title: "profile_camera".localize(), style: .default, handler: { _ in
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                imagePickerController.sourceType = .camera
                self.present(imagePickerController, animated: true, completion: nil)
            }
        }))
        actionsheet.addAction(UIAlertAction(title: "profile_library".localize(), style: .default, handler: { _ in
            imagePickerController.sourceType = .photoLibrary
            self.present(imagePickerController, animated: true, completion: nil)
        }))
        actionsheet.addAction(UIAlertAction(title: "profile_remove_image".localize(), style: .default, handler: { _ in
            imagePickerController.sourceType = .photoLibrary
            self.presenterImpl.setProfiilePicture(jpegData: nil)
        }))
        actionsheet.addAction(UIAlertAction(title: "cancel".localize(), style: .cancel, handler: nil))
        present(actionsheet, animated: true, completion: nil)
    }

    func imagePickerController(_ picker: UIImagePickerController,
                               didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        var image: UIImage?
        if let editedImage = info[.editedImage] as? UIImage {
            image = editedImage
        } else if let originalImage = info[.originalImage] as? UIImage {
            image = originalImage
        }
        if let jpegData = image?.jpegData(compressionQuality: 0.8) {
            self.presenterImpl.setProfiilePicture(jpegData: jpegData)
        }

        picker.dismiss(animated: true)
    }
}

// MARK: Custom notifications to reload attributes

extension Notification.Name {
    static let reloadAttributes = Notification.Name("reloadAttributes")
}
