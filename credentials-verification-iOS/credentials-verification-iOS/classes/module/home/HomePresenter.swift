//

class HomePresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, NewDegreeViewCellPresenterDelegate, DegreeViewCellPresenterDelegate, NewDegreeHeaderViewCellPresenterDelegate, DocumentViewCellPresenterDelegate, DetailHeaderViewCellPresenterDelegate, DetailFooterViewCellPresenterDelegate, DetailPropertyViewCellPresenterDelegate, ShareDialogPresenterDelegate {

    var viewImpl: HomeViewController? {
        return view as? HomeViewController
    }

    enum HomeMode {
        case degrees
        case document
        case detail
    }

    enum HomeCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case newDegreeHeader // degrees mode
        case newDegree // degree mode
        case document // document mode
        case detailHeader // detail mode
        case detailProperty // detail mode
        case detailFooter // detail mode
    }

    struct CellRow {
        var type: HomeCellType
        var value: Any?
    }

    var mode: HomeMode = .degrees

    var degreeRows: [CellRow]?
    var detailRows: [CellRow]?

    var detailDegree: Degree?

    var shareEmployers: [Employer]?
    var shareSelectedEmployers: [Employer]?

    // MARK: Modes

    func getMode() -> HomeMode {
        return mode
    }

    func startShowingDegrees() {

        mode = .degrees
        updateViewToState()
    }

    func startShowingDocument() {

        mode = .document
        updateViewToState()
    }

    func startShowingDetails(degree: Degree) {

        // Make the rows
        detailRows = []
        detailDegree = degree
        detailRows?.append(CellRow(type: .detailHeader, value: degree))
        degree.properties?.forEach { key, value in
            detailRows?.append(CellRow(type: .detailProperty, value: (key, value)))
        }
        detailRows?.append(CellRow(type: .detailFooter, value: degree))

        mode = .detail
        updateViewToState()
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode != .degrees {
            startShowingDegrees()
            return true
        }
        return false
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        detailDegree = nil
        degreeRows = []
        detailRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .degrees:
            return (degreeRows?.size() ?? 0) > 0
        case .detail:
            return (detailRows?.size() ?? 0) > 0
        case .document:
            return true
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .degrees:
            return (degreeRows?.size() ?? 0)
        case .document:
            return 1
        case .detail:
            return (detailRows?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> HomeCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .degrees:
            return degreeRows![indexPath.row].type
        case .document:
            return .document
        case .detail:
            return detailRows![indexPath.row].type
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    var showAsEmpty_DELETE_ME = false

    func fetchElements() {
        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            self.cleanData()

            // Fake data
            let degrees = FakeData.degreesList()
            self.parseData(degrees: degrees)

            DispatchQueue.main.async {
                self.startListing()
            }
        }
    }

    private func parseData(degrees: [Degree]) {

        let newDeg = degrees.filter { $0.isNew ?? false }
        let oldDeg = degrees.filter { !($0.isNew ?? false) }

        // Transform data into rows
        if newDeg.size() > 0 {
            self.degreeRows?.append(CellRow(type: .newDegreeHeader, value: nil))
        }
        newDeg.forEach { degree in
            self.degreeRows?.append(CellRow(type: .newDegree, value: degree))
        }
        oldDeg.forEach { degree in
            self.degreeRows?.append(CellRow(type: .degree, value: degree))
        }
        // NOTE: Won't add the National Id for now
        if getLoggedUser()?.identityNumber != nil {
            // self.degreeRows?.append(CellRow(type: .degree, value: getLoggedUser()))
        }
    }

    private func fetchShareEmployers() {

        viewImpl?.config(isLoading: true)

        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(0)

            self.shareEmployers = []
            self.shareSelectedEmployers = []

            // Fake data
            let employers = FakeData.employersList()
            self.shareEmployers?.append(contentsOf: employers)

            DispatchQueue.main.async {
                self.viewImpl?.config(isLoading: false)
                self.viewImpl?.showShareDialog()
            }
        }
    }

    private func shareWithSelectedEmployers() {
        // TODO:

        viewImpl?.config(isLoading: true)

        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            // Fake data
            // let employers = FakeData.employersList()
            // self.shareEmployers?.append(contentsOf: employers)

            // TODO: Check if share successfull

            DispatchQueue.main.async {
                self.viewImpl?.config(isLoading: false)
                self.viewImpl?.showSuccessMessage(doShow: true, message: "home_detail_share_success".localize())
            }
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        self.startShowingDegrees()
        self.fetchData()
        self.updateViewToState()
    }

    func setup(for cell: NewDegreeViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex], let degree = cellRow.value as? Degree else {
            return
        }

        var isLast = degreeRows!.count - 1 == rowIndex
        if !isLast {
            switch degreeRows![rowIndex + 1].type {
            case .newDegree:
                isLast = false
            default:
                isLast = true
            }
        }

        cell.config(title: degree.name, subtitle: degree.subtitle, logoUrl: degree.preLogoUrl, logoPlaceholderNamed: "ico_placeholder_university", isLast: isLast)
    }

    func tappedAction(for cell: NewDegreeViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex], let degree = cellRow.value as? Degree else {
            return
        }
        startShowingDetails(degree: degree)
    }

    func setup(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let degree = cellRow?.value as? Degree {
            cell.config(title: degree.name, subtitle: degree.subtitle, logoUrl: degree.preLogoUrl, logoPlaceholderNamed: "ico_placeholder_university")
        }
        // Config for an Id
        else if let _ = cellRow?.value as? LoggedUser {
            cell.config(title: "home_document_title".localize(), subtitle: nil, logoUrl: nil, logoPlaceholderNamed: "ico_placeholder_credential")
        }
    }

    func tappedAction(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let degree = cellRow?.value as? Degree {
            startShowingDetails(degree: degree)
        }
        // Config for an Id
        else if let _ = cellRow?.value as? LoggedUser {
            startShowingDocument()
        }
    }

    func setup(for cell: NewDegreeHeaderViewCell) {
        cell.config(name: getLoggedUser()?.firstName)
    }

    func setup(for cell: DocumentViewCell) {
        // TODO: Add when Document types are required
    }

    func setup(for cell: DetailHeaderViewCell) {
        cell.config(title: "home_detail_degree_name".localize(), subtitle: detailDegree?.fullName, logoUrl: detailDegree?.logoUrl)
    }

    func setup(for cell: DetailPropertyViewCell) {
        let detailRow = detailRows![cell.indexPath!.row]
        let pair = detailRow.value as! (String, String)
        cell.config(title: pair.0, subtitle: pair.1)
    }

    func setup(for cell: DetailFooterViewCell) {
        cell.config(startDate: detailDegree?.startDate, endDate: detailDegree?.endDate, isNew: detailDegree?.isNew ?? false)
    }

    func tappedDeclineAction(for cell: DetailFooterViewCell) {
        startShowingDegrees()
    }

    func tappedConfirmAction(for cell: DetailFooterViewCell) {
        // viewImpl?.changeScreenToPayment(degree: detailDegree)
        // TODO: Call server
        tappedBackButton()
    }

    // MARK: Share

    func tappedShareButton() {
        fetchShareEmployers()
    }

    func tappedDeclineAction(for view: ShareDialogViewController) {
        // Do nothing
    }

    func tappedConfirmAction(for view: ShareDialogViewController) {
        shareWithSelectedEmployers()
    }

    func shareItem(for view: ShareDialogViewController, at index: Int) -> Any? {
        return shareEmployers?[index]
    }

    func shareItemCount(for view: ShareDialogViewController) -> Int {
        return shareEmployers?.count ?? 0
    }

    func shareItemTapped(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployers![index]
        if employerIsSelected(employer: employer) {
            shareSelectedEmployers?.remove(employer)
        } else {
            shareSelectedEmployers?.append(employer)
        }
        viewImpl?.configShareDialog(enableButton: (shareSelectedEmployers?.count ?? 0) > 0)
        // TODO: Refresh better
        shareItemConfig(for: cell, at: index, item: item)
        cell?.refreshView()
    }

    func employerIsSelected(employer: Employer) -> Bool {
        return shareSelectedEmployers?.contains(where: { $0.id == employer.id }) ?? false
    }

    func shareItemConfig(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployers![index]
        let isSelected = employerIsSelected(employer: employer)
        cell?.config(name: employer.name, logoUrl: employer.logoUrl, placeholderNamed: "ico_placeholder_employer", isSelected: isSelected)
    }
}
