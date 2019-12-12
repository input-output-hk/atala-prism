//

protocol TabsViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: TabsViewCell)
}

class TabsViewCell: BaseTableViewCell {

    override class func default_NibName() -> String {
        return "TabsViewCell"
    }

    var delegateImpl: TabsViewCellPresenterDelegate? {
        return delegate as? TabsViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config() {}
}
