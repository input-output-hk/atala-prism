//

extension UITableView {

    func makeSimpleMarginHeader(height: CGFloat) {

        let tableHeader = UIView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: height))
        tableHeader.backgroundColor = UIColor.clear
        self.tableHeaderView = tableHeader
    }

    func makeCell(_ tableView: UITableView, identifier: String, nibName: String? = nil) -> UITableViewCell {

        var cell = tableView.dequeueReusableCell(withIdentifier: identifier)
        if cell == nil {
            if nibName != nil {
                tableView.register(UINib(nibName: nibName!, bundle: nil), forCellReuseIdentifier: identifier)
            }
            cell = tableView.dequeueReusableCell(withIdentifier: identifier)
        }
        return cell!
    }
}

extension UITableViewCell {

    class func makeCell<T: UITableViewCell>(_ type: T.Type, _ tableView: UITableView, identifier: String? = nil) -> T {

        let identif = identifier == nil ? default_NibName() : identifier!
        var cell = tableView.dequeueReusableCell(withIdentifier: identif) as? T
        if cell == nil {
            tableView.register(UINib(nibName: default_NibName(), bundle: nil), forCellReuseIdentifier: identif)
            cell = tableView.dequeueReusableCell(withIdentifier: identif) as? T
        }
        return cell!
    }

    // Must be overriden
    @objc class func default_NibName() -> String {
        assert(false)
        return ""
    }
}
