//

class ProfilePresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                        ProfileViewCellPresenterDelegate, TabsViewCellPresenterDelegate,
                        FieldViewCellPresenterDelegate {

    var viewImpl: ProfileViewController? {
        return view as? ProfileViewController
    }

    enum ProfileMode {
        case initial
        case editing
    }

    enum ProfileCellType {
        case base(value: ListingBaseCellType)
        case profile // initial mode
        case tabs // initial mode
        case field // initial mode
        case footer // initial mode
    }

    struct CellRow {
        var type: ProfileCellType
        var value: Any?
    }

    var mode: ProfileMode = .initial

    var initialRows: [CellRow]?
    var editingRows: [CellRow]?

    var editedFullname: String?
    var editedEmail: String?
    var editedCountry: String?

    // MARK: Modes

    func getMode() -> ProfileMode {
        return mode
    }

    lazy var initialStaticCells: [CellRow] = [
        CellRow(type: .profile, value: nil),
        // CellRow(type: .tabs, value: nil),
        CellRow(type: .field, value: (0, true)),
        CellRow(type: .field, value: (1, true)),
        CellRow(type: .field, value: (2, false)),
        CellRow(type: .footer, value: nil)
    ]

    func startShowingInitial() {

        mode = .initial
        cleanData()
        initialStaticCells.forEach { initialRows?.append($0) }
        updateViewToState()
    }

    func startShowingEdit() {

        mode = .editing
        cleanData()
        initialStaticCells.forEach { editingRows?.append($0) }
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
        if let fullName = editedFullname {
            let nameSplited = fullName.split(separator: " ", maxSplits: 1).map(String.init)
            sharedMemory.loggedUser?.firstName = nameSplited.size() > 0 ? nameSplited[0] : ""
            sharedMemory.loggedUser?.lastName = nameSplited.size() > 1 ? nameSplited[1] : ""
        }
        if let email = editedEmail, email.isEmail() {
            sharedMemory.loggedUser?.email = email
        }
        if let country = editedCountry {
            sharedMemory.loggedUser?.countryShortName = country
        }
        sharedMemory.loggedUser = sharedMemory.loggedUser
        // Go back
        tappedBackButton()
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
        editingRows = []

        editedFullname = nil
        editedEmail = nil
        editedCountry = nil
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

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .initial:
            return (initialRows?.size() ?? 0)
        case .editing:
            return (editingRows?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> ProfileCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .initial:
            return initialRows![indexPath.row].type
        case .editing:
            return editingRows![indexPath.row].type
        }
    }

    // MARK: Fetch

    func fetchElements() {

        switch mode {
        case .initial:
            self.startShowingInitial()
            self.startListing()
        case .editing:
            self.startShowingEdit()
            self.startListing()
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        self.fetchData()
        self.updateViewToState()
    }

    func setup(for cell: ProfileViewCell) {

        let user = sharedMemory.loggedUser
        let name = "\(user?.firstName ?? "") \(user?.lastName ?? "")"
        let email = "\(user?.email ?? "")"
        cell.config(name: name, email: email, isVerified: user?.isVerified ?? false, logoUrl: user?.avatarUrl)
    }

    func setup(for cell: TabsViewCell) {
        cell.config()
    }

    private func getFieldCellRowValue(index: IndexPath) -> (Int, Bool)? {
        return (mode == .initial ? initialRows?[index.row] : editingRows?[index.row])?.value as? (Int, Bool)
    }

    func setup(for cell: FieldViewCell) {

        let user = sharedMemory.loggedUser
        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!

        var fieldTitle: String?
        var fieldText: String?
        switch fieldValue.0 {
        case 0:
            fieldTitle = "profile_field_title_fullname".localize()
            fieldText = "\(user?.firstName ?? "") \(user?.lastName ?? "")"
        case 1:
            fieldTitle = "profile_field_title_country".localize()
            fieldText = "\(user?.countryShortName ?? "")"
        case 2:
            fieldTitle = "profile_field_title_email".localize()
            fieldText = "\(user?.email ?? "")"
        default:
            break
        }
        cell.config(title: fieldTitle, text: fieldText, bgColor: UIColor.appGreyLight,
                    isEnable: mode == .editing, hasNext: fieldValue.1)
    }

    func textFieldDidChange(for cell: FieldViewCell, text: String?) {

        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!
        switch fieldValue.0 {
        case 0:
            editedFullname = text
        case 1:
            editedCountry = text
        case 2:
            editedEmail = text
        default:
            break
        }
    }

    func textFieldShouldReturn(for cell: FieldViewCell) -> (Bool, FieldViewCell?) {

        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!
        var nextCell: FieldViewCell?
        if fieldValue.1 {
            var nextIndex = cell.indexPath!
            nextIndex.row += 1
            nextCell = viewImpl?.table.cellForRow(at: nextIndex) as? FieldViewCell
        }
        return (!fieldValue.1, nextCell)
    }
}
