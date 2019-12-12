// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
import UIKit

extension UISearchBar {

    var textField: UITextField? {
        return value(forKey: "searchField") as? UITextField
    }

    func setSearchIcon(image: UIImage) {
        setImage(image, for: .search, state: .normal)
    }

    func setClearIcon(image: UIImage) {
        setImage(image, for: .clear, state: .normal)
    }
}
