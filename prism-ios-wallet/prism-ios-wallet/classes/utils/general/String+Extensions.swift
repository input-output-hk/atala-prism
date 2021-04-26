//

//  The following extension allows a String literal to return the localized version of
//  itself.
//
extension String {

    func localize() -> String {
        return NSLocalizedString(self, comment: "")
    }

    func localizeHTML(pointSize: CGFloat = 12, alignment: NSTextAlignment = .left) -> NSAttributedString? {

        do {
            let localizedStr: String = self.localize()

            let modifiedFont = NSString(format:
                "<span style=\"font-family: '-apple-system', '.SFUIText'; font-size: \(pointSize)\">%@</span>"
                    as NSString, localizedStr) as String

            let attrStr = try NSMutableAttributedString(
                data: modifiedFont.data(using: String.Encoding.unicode, allowLossyConversion: true)!,
                options: [NSAttributedString.DocumentReadingOptionKey.documentType:
                    NSAttributedString.DocumentType.html],
                documentAttributes: nil
            )

            let style = NSMutableParagraphStyle()
            style.alignment = alignment
            attrStr.addAttributes([NSAttributedString.Key.paragraphStyle: style],
                                  range: NSRange(location: 0, length: attrStr.length))

            return attrStr
        } catch _ {}
        return nil
    }
}

extension String {

    static func fromJson(_ json: [String: Any]) -> String? {

        if let theJSONData = try? JSONSerialization.data(withJSONObject: json, options: []),
            let theJSONText = String(data: theJSONData, encoding: String.Encoding.ascii) {
            return theJSONText
        }
        return nil
    }

    func toJson() -> Any? {

        if let data = self.data(using: .utf8) {
            do {
                return try JSONSerialization.jsonObject(with: data, options: [])
            } catch {
                Logger.e(error.localizedDescription)
            }
        }
        return nil
    }
}

//  The following extension allows the use of simple integers when defining substring
//  ranges.
//
extension String {

    func index(from: Int) -> Index {
        return self.index(startIndex, offsetBy: from)
    }

    func substring(from: Int) -> String {

        let fromIndex = index(from: from)
        return String(self.suffix(from: fromIndex))
    }

    func substring(toPos: Int) -> String {

        let toIndex = index(from: toPos)
        return String(self[..<toIndex])
    }

    func substring(with range: Range<Int>) -> String {

        let startIndex = index(from: range.lowerBound)
        let endIndex = index(from: range.upperBound)
        return String(self[startIndex ..< endIndex])
    }

    func isEmail() -> Bool {

        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return NSPredicate(format: "SELF MATCHES %@", emailRegex).evaluate(with: self)
    }

    func isPhone() -> Bool {

        let phoneRegex = "^((\\+)|(00))[0-9]{6,14}$"
        let numberRegex = "^[0-9]+$"
        return
            NSPredicate(format: "SELF MATCHES %@", phoneRegex).evaluate(with: self)
            || NSPredicate(format: "SELF MATCHES %@", numberRegex).evaluate(with: self)
    }

    static func isEmpty(_ target: String?) -> Bool {
        return target == nil || target!.isEmpty
    }

    func trim() -> String {
        return self.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func truncate(_ maxCount: Int) -> String {

        if self.count <= maxCount {
            return self
        }
        let temp = self.substring(toPos: maxCount)
        return "\(temp)…"
    }

    func attributedStringWithColor(_ strings: [String], color: UIColor,
                                   characterSpacing: UInt? = nil) -> NSAttributedString {
        let attributedString = NSMutableAttributedString(string: self)
        for string in strings {
            let range = (self as NSString).range(of: string)
            attributedString.addAttribute(NSAttributedString.Key.foregroundColor, value: color, range: range)
        }

        guard let characterSpacing = characterSpacing else { return attributedString }

        attributedString.addAttribute(NSAttributedString.Key.kern, value: characterSpacing,
                                      range: NSRange(location: 0, length: attributedString.length))

        return attributedString
    }
    
    func addBoldText(fullString: NSString, boldPartsOfString: Array<NSString>, font: UIFont!, boldFont: UIFont!) -> NSAttributedString {
        let nonBoldFontAttribute = [NSAttributedString.Key.font:font!]
        let boldFontAttribute = [NSAttributedString.Key.font:boldFont!]
        let boldString = NSMutableAttributedString(string: fullString as String, attributes:nonBoldFontAttribute)
        for index in 0 ..< boldPartsOfString.count {
            boldString.addAttributes(boldFontAttribute, range: fullString.range(of: boldPartsOfString[index] as String))
        }
        return boldString
    }
}

extension String {

    init?(htmlEncodedString: String?) {
        guard let str = htmlEncodedString else {
            return nil
        }

        guard let data = str.data(using: .utf8) else {
            return nil
        }

        let options: [NSAttributedString.DocumentReadingOptionKey: Any] = [
            .documentType: NSAttributedString.DocumentType.html,
            .characterEncoding: String.Encoding.utf8.rawValue
        ]

        guard let attributedString = try? NSAttributedString(data: data, options: options,
                                                             documentAttributes: nil) else {
            return nil
        }

        self.init(attributedString.string)

    }

}
