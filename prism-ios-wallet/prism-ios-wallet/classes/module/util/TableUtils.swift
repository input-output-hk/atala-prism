//

protocol TableUtilsPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func getSectionCount() -> Int?
    func getSectionHeaderViews() -> [UIView]
    func getElementCount() -> [Int]
    func hasPullToRefresh() -> Bool
    func actionPullToRefresh()
    func didSelectRowAt(indexPath: IndexPath)
}

extension TableUtilsPresenterDelegate {

    func didSelectRowAt(indexPath: IndexPath) {
        //this is a empty implementation to allow this method to be optional
    }
}

protocol TableUtilsViewDelegate: class {

    func getCellIdentifier(for indexPath: IndexPath) -> String
    func getCellNib(for indexPath: IndexPath) -> String?
    func getHeaderHeight(for section: Int) -> CGFloat
}

class TableUtils: NSObject, UITableViewDataSource, UITableViewDelegate {

    weak var viewDelegate: TableUtilsViewDelegate!
    weak var presenterDelegate: TableUtilsPresenterDelegate!
    weak var table: UITableView!
    private let refreshControl = UIRefreshControl()

    init(view: TableUtilsViewDelegate, presenter: TableUtilsPresenterDelegate?, table: UITableView) {
        super.init()

        self.viewDelegate = view
        self.presenterDelegate = presenter
        self.table = table

        self.table.delegate = self
        self.table.dataSource = self

        if self.presenterDelegate.hasPullToRefresh() {
            setupPullToRefresh()
        }
        self.table.refreshControl = refreshControl
    }

    func refreshTable() {

        table.reloadData()
    }

    func refreshTableCell(indexPath: IndexPath?) {

        if indexPath == nil {
            return
        }
        table.reloadRows(at: [indexPath!], with: UITableView.RowAnimation.none)
    }

    func isLastElement(indexPath: IndexPath) -> Bool {
        return presenterDelegate.getElementCount()[indexPath.section] == indexPath.section
//        return false
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return viewDelegate.getHeaderHeight(for: section)
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return presenterDelegate.getSectionHeaderViews()[section]
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        let identifier = viewDelegate.getCellIdentifier(for: indexPath)
        let nibName = viewDelegate.getCellNib(for: indexPath)
        let cell = tableView.makeCell(tableView, identifier: identifier, nibName: nibName)
        if let baseCell = cell as? BaseTableViewCell {
            baseCell.initialSetup(index: indexPath, delegate: presenterDelegate)
        }
        return cell
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let rows = presenterDelegate.getElementCount()
        return section < rows.count ? rows[section] : 0
    }

    func numberOfSections(in tableView: UITableView) -> Int {
        return presenterDelegate.getSectionCount() ?? 1
    }

    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        if let baseCell = cell as? BaseTableViewCell {
            baseCell.willDisplay()
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        presenterDelegate.didSelectRowAt(indexPath: indexPath)
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat{
        return 0.01;
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }

    // MARK: Pull to refresh

    func setupPullToRefresh() {

        if #available(iOS 10.0, *) {
            self.table.refreshControl = refreshControl
        } else {
            self.table.addSubview(refreshControl)
        }
        refreshControl.tintColor = UIColor.clear
        refreshControl.addTarget(self, action: #selector(actionPullToRefresh(_:)), for: .valueChanged)
    }

    @objc private func actionPullToRefresh(_ sender: Any) {

        self.refreshControl.endRefreshing()
        self.table.scrollToNearestSelectedRow(at: .top, animated: true)
        self.presenterDelegate.actionPullToRefresh()
    }
}
