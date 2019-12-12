//

import QuartzCore

@IBDesignable extension UILabel {

    @IBInspectable var localized: String {
        set {
            self.text = newValue.localize()
        }
        get {
            return self.text ?? ""
        }
    }

    @IBInspectable var localizedHTML: String {
        set {
            let attributed = newValue.localizeHTML(pointSize: self.font.pointSize,
                                                   alignment: self.textAlignment)
            self.attributedText = attributed
        }
        get {
            return self.text ?? ""
        }
    }
}

@IBDesignable extension UIButton {

    @IBInspectable var localized: String {
        set {
            self.setTitle(newValue.localize(), for: .normal)
        }

        get {
            return self.title(for: .normal) ?? ""
        }
    }

    @IBInspectable var localizedHTML: String {
        set {
            let attributed = newValue.localizeHTML(pointSize: (self.titleLabel?.font.pointSize) ?? 12,
                                                   alignment: self.alignmentConvert(self.contentHorizontalAlignment))
            self.setAttributedTitle(attributed, for: .normal)
        }

        get {
            return self.title(for: .normal) ?? ""
        }
    }

    func alignmentConvert(_ original: UIControl.ContentHorizontalAlignment) -> NSTextAlignment {

        switch original {
        case .center:
            return NSTextAlignment.center
        case .right:
            return NSTextAlignment.right
        default:
            return NSTextAlignment.left
        }
    }
}
