//

class WalletPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, CommonViewCellPresenterDelegate, HistoryViewCellPresenterDelegate {

    var viewImpl: WalletViewController? {
        return view as? WalletViewController
    }

    enum WalletMode {
        case initial
        case history
        case creditCards
    }

    enum WalletCellType {
        case base(value: ListingBaseCellType)
        case initial // initial mode
        case history // history mode
        case cards // creditCards mode
    }

    struct CellRow {
        var type: WalletCellType
        var value: Any?
    }

    struct InitialCellValue {
        var icon: String
        var title: String
        var action: SelectorAction?
    }

    var mode: WalletMode = .initial

    var initialRows: [CellRow]?
    var historyRows: [CellRow]?
    var cardsRows: [CellRow]?

    // MARK: Modes

    func getMode() -> WalletMode {
        return mode
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(icon: "logo_credit_card", title: "wallet_initial_row_credit_cards", action: nil),
        InitialCellValue(icon: "logo_sp_money", title: "wallet_initial_row_digital_lari", action: nil),
        InitialCellValue(icon: "logo_dollar", title: "wallet_initial_row_cash", action: nil),
        InitialCellValue(icon: "logo_bank", title: "wallet_initial_row_wire_transfer", action: nil),
    ]

    func startShowingInitial() {

        mode = .initial
        initialRows = []
        initialStaticCells.forEach { initialRows?.append(CellRow(type: .initial, value: $0)) }
        updateViewToState()
    }

    func startShowingHistory() {

        mode = .history
        fetchData()
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

    func tappedHistoryButton() {
        startShowingHistory()
    }

    func tappedHistoryEmptyButton() {
        tappedBackButton()
    }

    func tappedCardsEmptyButton() {
        // TODO: Implement me
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
        historyRows = []
        cardsRows = []
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
        case .history:
            return (historyRows?.size() ?? 0) > 0
        case .creditCards:
            return (cardsRows?.size() ?? 0) > 0
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .initial:
            return (initialRows?.size() ?? 0)
        case .history:
            return (historyRows?.size() ?? 0)
        case .creditCards:
            return (cardsRows?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> WalletCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .initial:
            return initialRows![indexPath.row].type
        case .history:
            return historyRows![indexPath.row].type
        case .creditCards:
            return cardsRows![indexPath.row].type
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    var showAsEmpty_DELETE_ME = false

    func fetchElements() {

        switch mode {
        case .initial:
            self.startShowingInitial()
            self.startListing()
        case .history:
            return fetchHistory()
        case .creditCards:
            return fetchCreditCards()
        }
    }

    private func fetchHistory() {

        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            self.cleanData()

            // Fake data
            let history = FakeData.paymentHistoryList()
            self.parseData(history: history)

            DispatchQueue.main.async {
                self.startListing()
            }
        }
    }

    private func fetchCreditCards() {
        // TODO:
    }

    private func parseData(history: [PaymentHistory]) {

        historyRows = []
        history.forEach { historyElem in
            historyRows?.append(CellRow(type: .history, value: historyElem))
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

        let value = initialRows![cell.indexPath!.row].value as! InitialCellValue
        cell.config(title: value.title.localize(), subtitle: nil, logoData: nil, logoPlaceholderNamed: value.icon, isComingSoon: value.action == nil)
    }

    func tappedAction(for cell: CommonViewCell) {

        let value = initialRows![cell.indexPath!.row].value as! InitialCellValue
        value.action?.action()
    }

    func setup(for cell: HistoryViewCell) {

        let value = historyRows![cell.indexPath!.row].value as! PaymentHistory
        cell.config(title: value.text, date: value.date, amount: value.amount, status: value.status ?? 0)
    }
}
