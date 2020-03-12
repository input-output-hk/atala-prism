//

class CredentialPayPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, CredentialPayViewCellPresenterDelegate, FieldViewCellPresenterDelegate, ConfirmationViewCellPresenterDelegate {

    var viewImpl: CredentialPayViewController? {
        return view as? CredentialPayViewController
    }

    enum CredentialPayMode {
        case initial
    }

    enum CredentialPayCellType {
        case base(value: ListingBaseCellType)
        case credentialPay // initial mode
        case field // initial mode
        case confirmation // initial mode
    }

    struct CellRow {
        var type: CredentialPayCellType
        var value: Any?
    }

    var mode: CredentialPayMode = .initial

    var initialRows: [CellRow]?

    var cardNumber: String?
    var cardDate: String?
    var cardCvv: String?
    var cardName: String?

    // MARK: Modes

    func getMode() -> CredentialPayMode {
        return mode
    }

    lazy var initialStaticCells: [CellRow] = [
        CellRow(type: .credentialPay, value: nil),
        CellRow(type: .field, value: (0, true)),
        CellRow(type: .field, value: (1, true)),
        CellRow(type: .field, value: (2, true)),
        CellRow(type: .field, value: (3, false)),
        CellRow(type: .confirmation, value: nil),
    ]

    func startShowingInitial() {

        mode = .initial
        cleanData()
        initialStaticCells.forEach { initialRows?.append($0) }
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

    func getElementType(indexPath: IndexPath) -> CredentialPayCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .initial:
            return initialRows![indexPath.row].type
        }
    }

    // MARK: Fetch

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

        self.fetchData()
        self.updateViewToState()
    }

    func setup(for cell: CredentialPayViewCell) {

        cell.config(amount: "0")
    }

    func setup(for cell: TabsViewCell) {
        cell.config()
    }

    private func getFieldCellRowValue(index: IndexPath) -> (Int, Bool)? {
        return initialRows?[index.row].value as? (Int, Bool)
    }

    func setup(for cell: FieldViewCell) {

        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!

        var fieldTitle: String?
        switch fieldValue.0 {
        case 0:
            fieldTitle = "credentialpay_card_number".localize()
        case 1:
            fieldTitle = "credentialpay_card_expiration".localize()
        case 2:
            fieldTitle = "credentialpay_card_cvv".localize()
        case 3:
            fieldTitle = "credentialpay_card_name".localize()
        default:
            break
        }
        cell.config(title: fieldTitle, text: "", bgColor: UIColor.appWhite, hasNext: fieldValue.1)
    }

    func textFieldDidChange(for cell: FieldViewCell, text: String?) {

        let fieldValue = getFieldCellRowValue(index: cell.indexPath!)!
        switch fieldValue.0 {
        case 0:
            cardNumber = text
        case 1:
            cardDate = text
        case 2:
            cardCvv = text
        case 3:
            cardName = text
        default:
            break
        }
        // Refresh the last cell
        let indexPath = IndexPath(row: getElementCount() - 1, section: 0)
        viewImpl?.tableUtils?.refreshTableCell(indexPath: indexPath)
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

    func setup(for cell: ConfirmationViewCell) {

        let isActive = ((cardNumber?.count ?? 0) > 0) && ((cardDate?.count ?? 0) > 0) && ((cardCvv?.count ?? 0) > 0) && ((cardName?.count ?? 0) > 0)
        cell.config(title: "continue".localize(), isEnabled: isActive)
    }

    func tappedAction(for cell: ConfirmationViewCell) {

        Tracker.global.trackPaymentConfirmTapped()
        // TODO: Call WS
        viewImpl?.changeScreenToSuccess(action: actionSuccessContinue)
    }

    lazy var actionSuccessContinue = SelectorAction(action: { [weak self] in
        self?.viewImpl?.closePaymentFlow()
    })
}
