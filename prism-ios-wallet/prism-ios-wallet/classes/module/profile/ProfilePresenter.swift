//

class ProfilePresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                        CommonViewCellPresenterDelegate, ProfileViewCellPresenterDelegate,
                        HomeCardsTableViewCellDelegate {

    var viewImpl: ProfileViewController? {
        return view as? ProfileViewController
    }

    enum ProfileMode {
        case initial
        case editing
    }

    enum ProfileCellType {
        case base(value: ListingBaseCellType)
        case profile
        case initial
    }

    struct CellRow {
        var type: ProfileCellType
        var value: Any?
    }

    var mode: ProfileMode = .initial

    var initialRows: [CellRow]?
    var attributeRows: [CellRow]? = []
    var editingRows: [CellRow]?

    var attributes: [Attribute] = []
    var editedImage: Data?

    // MARK: Modes

    func getMode() -> ProfileMode {
        return mode
    }
    
    lazy var initialStaticCells: [CellRow] = [
        CellRow(type: .profile, value: nil),
        CellRow(type: .initial, value: (0, true))
    ]

    func startShowingInitial() {

        mode = .initial
        state = .listing
        cleanData()
        initialStaticCells.forEach { initialRows?.append($0) }

        var count: Int = initialRows?.count ?? 1

        for att in sharedMemory.loggedUser!.attributes {

            if !attributes.contains(where: { $0.type == att.type }) {

                attributes.append(att)
                attributeRows?.append(CellRow(type: .initial, value: (count, true)))
                attributeRows?.forEach { initialRows?.append($0) }
                count+=1
            }
        }
        updateViewToState()
    }

    func startShowingEdit() {

        mode = .editing
        cleanData()
        editedImage = sharedMemory.profilePic
        initialStaticCells.forEach { editingRows?.append($0) }

        var count: Int = editingRows?.count ?? 1

        for att in sharedMemory.loggedUser!.attributes {

            if !attributes.contains(where: { $0.type == att.type }) {

                attributes.append(att)
                attributeRows?.append(CellRow(type: .initial, value: (count, true)))
                attributeRows?.forEach { editingRows?.append($0) }
                count+=1
            }
        }
        updateViewToState()
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode != .initial {
            startShowingInitial()
            return true
        }
        return false
    }

    func tappedEditButton() {

        startShowingEdit()
    }

    func tappedEditSaveButton() {
        // Save the results
        sharedMemory.profilePic = editedImage
        sharedMemory.loggedUser = sharedMemory.loggedUser
        // Go back
        tappedBackButton()
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
        attributeRows = []
        attributes = []
        editingRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .initial:
            return (initialRows?.size() ?? 0) > 0
        case .editing:
            return (editingRows?.size() ?? 0) > 0
        }
    }

    func getSectionCount() -> Int? {
        return attributeRows?.count > 0 ? 3 : 2
    }

    func getSectionHeaderViews() -> [UIView] {

        var arrViews = [UIView]()

        let numOfSections: Int = viewImpl?.table.numberOfSections ?? 1

        for section in 0..<numOfSections {
            switch section {
            case 0:
                arrViews.append(returnTalbeviewheaderWith(title: nil))
            case 1:
                arrViews.append(returnTalbeviewheaderWith(title: "profile_table_header_title".localize()))
            case 2:
                arrViews.append(returnTalbeviewheaderWith(title: "profile_table_header_title_social".localize()))
            default:
                break
            }
        }
        return arrViews
    }

    func returnTalbeviewheaderWith(title: String?) -> UIView {

        let headerView = UIView()

        if title != nil {

            headerView.backgroundColor = .white

            let sectionLabel = UILabel(frame: CGRect(x: 12, y: 16, width: viewImpl!.table.bounds.size.width,
                                                     height: viewImpl!.table.bounds.size.height))
            sectionLabel.font = UIFont(name: "Helvetica", size: 12)
            sectionLabel.textColor = .lightGray
            sectionLabel.text = title
            sectionLabel.sizeToFit()
            headerView.addSubview(sectionLabel)
        }

        return headerView
    }

    func getElementCount() -> [Int] {

        var arrElements = [Int]()

        let numOfSections: Int = viewImpl?.table.numberOfSections ?? 1

        for section in 0..<numOfSections {
            switch section {
            case 0:
                arrElements.append(2)
            case 1:
                arrElements.append(1)
            case 2:
                arrElements.append(attributeRows?.count ?? 1)
            default:
                break
            }
        }
        return arrElements
    }

    func getElementType(indexPath: IndexPath) -> ProfileCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .initial:
            return initialRows![indexPath.section].type
        case .editing:
            return editingRows![indexPath.section].type
        }
    }

    // MARK: Fetch

    func fetchElements() {

        switch mode {
        case .initial:
            self.startShowingInitial()
        case .editing:
            self.startShowingEdit()
            self.startListing()
        }
    }

    func addAttributeWith(type: String, logo: String) {
        viewImpl?.changeScreenToAttributeVerification(type: type, logo: logo)
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {}

    func setup(for cell: ProfileViewCell) {

        let user = sharedMemory.loggedUser
        let name = "\(user?.firstName ?? "") \(user?.lastName ?? "")"
        let email = "\(user?.email ?? "")"
        let pic = mode == .editing ? editedImage : sharedMemory.profilePic
        cell.config(name: name, email: email, isVerified: user?.isVerified ?? false, logoData: pic,
                    isEnable: mode == .editing)
    }

    func choosePicture() {
        viewImpl?.chooseProfilePicture()
    }

    func setup(for cell: TabsViewCell) {
        cell.config()
    }

    private func getFieldCellRowValue(index: IndexPath) -> (Int, Bool)? {
        return (mode == .initial ? initialRows?[index.section] : editingRows?[index.section])?.value as? (Int, Bool)
    }

    func setProfiilePicture(jpegData: Data?) {
        editedImage = jpegData
        updateViewToState()
    }

    func setup(for cell: CommonViewCell) {

        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!

        var fieldTitle: String
        var logo: String
        let isVerified: Bool = true

        switch fieldValue.0 {
        case 0:
            fieldTitle = "profile_row_personal_info_title".localize()
            logo = "logo_security"
        default:
            fieldTitle = (attributes[cell.indexPath!.row].type) ?? ""
            logo = (attributes[cell.indexPath!.row].logo)  ?? ""
        }
        cell.config(title: fieldTitle, subtitle: nil, logoData: nil, logoPlaceholderNamed: logo, isVerified: isVerified)
    }

    func tappedAction(for cell: CommonViewCell) {
        rowActionFor(indexPath: cell.indexPath!)
    }

    func didSelectRowAt(indexPath: IndexPath) {
        rowActionFor(indexPath: indexPath)
    }

    func rowActionFor(indexPath: IndexPath) {
        switch indexPath.section {
        case 1:
            viewImpl?.changeScreenToProfileDetail()
        case 2:
            let type: String = (attributes[indexPath.row].type)  ?? ""
            viewImpl?.changeScreenToAttributeListing(type: type)
        default:
            break
        }
    }

    // MARK: HomeCardsTableViewCellDelegate

    func setup(for cell: HomeCardsTableViewCell) {
        let loggedUser = sharedMemory.loggedUser
        cell.config(hidePayId: loggedUser?.payIdCardDismissed ?? false,
                    hideVerifyId: loggedUser?.verifyIdCardDismissed ?? false, delegate: self)
    }

    func payIdTapped(for cell: HomeCardsTableViewCell) {
        // TODO: this will be implemented with the pay id functionality
    }

    func dismissPayIdTapped(for cell: HomeCardsTableViewCell) {
        sharedMemory.loggedUser?.payIdCardDismissed = true
        sharedMemory.loggedUser = sharedMemory.loggedUser
        viewImpl?.table.reloadData()
    }

    func verifyIdTapped(for cell: HomeCardsTableViewCell) {
        viewImpl?.changeScreenToVerifyId()
    }

    func dismissVerifyIdTapped(for cell: HomeCardsTableViewCell) {
        sharedMemory.loggedUser?.verifyIdCardDismissed = true
        sharedMemory.loggedUser = sharedMemory.loggedUser
        viewImpl?.table.reloadData()
    }
}
