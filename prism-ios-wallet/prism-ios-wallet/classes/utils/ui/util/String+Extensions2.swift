// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
// swiftlint:disable identifier_name
import UIKit

extension String {

    subscript(i: Int) -> Character {
        return self[index(startIndex, offsetBy: i)]
    }

    subscript(i: Int) -> String {
        return String(self[i] as Character)
    }

    subscript(r: Range<Int>) -> String {
        let start = index(startIndex, offsetBy: r.lowerBound)
        let end = index(startIndex, offsetBy: r.upperBound)
        return String(self[start ..< end])
    }

    var containsAlphabets: Bool {
        // Checks if all the characters inside the string are alphabets
        let set = CharacterSet.letters
        return self.utf16.contains {
            guard let unicode = UnicodeScalar($0) else { return false }
            return set.contains(unicode)
        }
    }
}

// MARK: NSAttributedString extensions

public extension String {

    /// Regular string.
    var regular: NSMutableAttributedString {
        return NSMutableAttributedString(string: self,
                                         attributes: [.font: UIFont.systemFont(ofSize: UIFont.systemFontSize)])
    }

    /// Bold string.
    var bold: NSMutableAttributedString {
        return NSMutableAttributedString(string: self,
                                         attributes: [.font: UIFont.boldSystemFont(ofSize: UIFont.systemFontSize)])
    }

    /// Underlined string
    var underline: NSMutableAttributedString {
        return NSMutableAttributedString(string: self, attributes: [.underlineStyle: NSUnderlineStyle.single.rawValue])
    }

    /// Strikethrough string.
    var strikethrough: NSMutableAttributedString {
        return NSMutableAttributedString(string: self,
                                  attributes: [.strikethroughStyle: NSNumber(value:
                                    NSUnderlineStyle.single.rawValue as Int)])
    }

    /// Italic string.
    var italic: NSMutableAttributedString {
        return NSMutableAttributedString(string: self,
                                         attributes: [.font: UIFont.italicSystemFont(ofSize: UIFont.systemFontSize)])
    }

    /// Add color to string.
    ///
    /// - Parameter color: text color.
    /// - Returns: a NSAttributedString versions of string colored with given color.
    func colored(with color: UIColor) -> NSMutableAttributedString {
        return NSMutableAttributedString(string: self, attributes: [.foregroundColor: color])
    }
}

extension Array where Element: NSAttributedString {
    func joined(separator: NSAttributedString) -> NSAttributedString {
        var isFirst = true
        return self.reduce(NSMutableAttributedString()) { r, e in
            if isFirst {
                isFirst = false
            } else {
                r.append(separator)
            }
            r.append(e)
            return r
        }
    }

    func joined(separator: String) -> NSAttributedString {
        return joined(separator: NSAttributedString(string: separator))
    }
}
