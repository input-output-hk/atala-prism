//

class SettingsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, CommonViewCellPresenterDelegate {

    var viewImpl: SettingsViewController? {
        return view as? SettingsViewController
    }

    enum SettingsMode {
        case initial
    }

    enum SettingsCellType {
        case base(value: ListingBaseCellType)
        case initial // initial mode
    }

    struct CellRow {
        var type: SettingsCellType
        var value: Any?
    }

    struct InitialCellValue {
        var icon: String
        var title: String
        var subtitle: String
        var action: SelectorAction?
    }

    var mode: SettingsMode = .initial

    var initialRows: [CellRow]?

    // MARK: Modes

    func getMode() -> SettingsMode {
        return mode
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(icon: "logo_backup", title: "settings_backup_title",
                         subtitle: "settings_backup_subtitle", action: nil),
        InitialCellValue(icon: "logo_reset", title: "settings_reset_title",
                        subtitle: "settings_reset_subtitle", action: actionRowReset),
        InitialCellValue(icon: "logo_security", title: "settings_security_title",
                         subtitle: "settings_security_subtitle", action: actionRowSecurity),
        InitialCellValue(icon: "logo_support", title: "settings_support_title",
                         subtitle: "settings_support_subtitle", action: nil),
        InitialCellValue(icon: "logo_about", title: "settings_about_title",
                         subtitle: "settings_about_subtitle", action: actionRowTerms)
    ]

    func startShowingInitial() {

        mode = .initial
        initialRows = []
        initialStaticCells.forEach { initialRows?.append(CellRow(type: .initial, value: $0)) }
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

    lazy var actionRowReset = SelectorAction(action: { [weak self] in
        self?.viewImpl?.resetData()
    })

    lazy var actionRowSupport = SelectorAction(action: { [weak self] in
        self?.viewImpl?.changeScreenToBrowser(urlStr: Common.URL_SUPPORT)
    })

    lazy var actionRowTerms = SelectorAction(action: { [weak self] in
        self?.viewImpl?.changeScreenToAbout()
    })

    lazy var actionRowSecurity = SelectorAction(action: { [weak self] in
        self?.viewImpl?.changeScreenToSecurity()
    })

    func clearAppData() {
        sharedMemory.loggedUser?.connectionUserIds?.removeAll()
        sharedMemory.loggedUser?.messagesAcceptedIds?.removeAll()
        sharedMemory.loggedUser?.messagesRejectedIds?.removeAll()
        sharedMemory.loggedUser = sharedMemory.loggedUser
        viewImpl?.showSuccessMessage(doShow: true, message: "settings_reset_success".localize(), title: "")
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
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
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .initial:
            return (initialRows?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> SettingsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .initial:
            return initialRows![indexPath.row].type
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    func fetchElements() {

        switch mode {
        case .initial:
            self.startShowingInitial()
            self.startListing()
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        // self.startShowingInitial()
        self.fetchData()
        self.updateViewToState()
    }

    func setup(for cell: CommonViewCell) {

        if let value = initialRows![cell.indexPath!.row].value as? InitialCellValue {
            cell.config(title: value.title.localize(), subtitle: value.subtitle.localize(),
                        logoData: nil, logoPlaceholderNamed: value.icon, isComingSoon: value.action == nil)
        }
    }

    func tappedAction(for cell: CommonViewCell) {

        if let value = initialRows![cell.indexPath!.row].value as? InitialCellValue {
            value.action?.action()
        }
    }

    func didSelectRowAt(indexPath: IndexPath) {
        if let value = initialRows![indexPath.row].value as? InitialCellValue {
            value.action?.action()
        }
    }
}
