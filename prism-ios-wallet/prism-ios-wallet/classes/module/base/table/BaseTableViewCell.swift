//

protocol BaseTableViewCellPresenterDelegate: class {}

class BaseTableViewCell: UITableViewCell {

    weak var delegate: BaseTableViewCellPresenterDelegate?
    var indexPath: IndexPath?

    func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {

        self.indexPath = index
        self.delegate = delegate
    }

    func willDisplay() {}
}
