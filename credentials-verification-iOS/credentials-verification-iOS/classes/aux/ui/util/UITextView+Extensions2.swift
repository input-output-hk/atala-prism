// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
import UIKit

// MARK: Methods

public extension UITextView {

    /// Scroll to the bottom of text view
    func scrollToBottom() {
        let range = NSMakeRange((text as NSString).length - 1, 1)
        scrollRangeToVisible(range)
    }

    /// Scroll to the top of text view
    func scrollToTop() {
        let range = NSMakeRange(0, 1)
        scrollRangeToVisible(range)
    }
}
