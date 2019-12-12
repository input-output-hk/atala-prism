extension UIColor {

    static func getColorByName(_ name: String?) -> UIColor? {
        if name != nil, let newColor: UIColor = UIColor.ColorNameDic[name!] {
            return newColor
        }
        return nil
    }

    convenience init(red: Int, green: Int, blue: Int) {
        self.init(red: CGFloat(red) / 255.0, green: CGFloat(green) / 255.0, blue: CGFloat(blue) / 255.0, alpha: 1.0)
    }

    convenience init(netHex: Int) {
        self.init(red: (netHex >> 16) & 0xFF, green: (netHex >> 8) & 0xFF, blue: netHex & 0xFF)
    }

    public func hexString(_ includeAlpha: Bool = true) -> String {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        self.getRed(&r, green: &g, blue: &b, alpha: &a)

        if includeAlpha {
            return String(format: "#%02X%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255), Int(a * 255))
        } else {
            return String(format: "#%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
        }
    }
}

// MARK: Extensions

@IBDesignable extension UIView {

    @IBInspectable var backgroundColorIB: String? {
        set {
            if let newColor = UIColor.getColorByName(newValue) {
                self.backgroundColor = newColor
            }
        }
        get { return "" }
    }
}

@IBDesignable extension UILabel {

    @IBInspectable var textColorIB: String? {
        set {
            if let newColor = UIColor.getColorByName(newValue) {
                self.textColor = newColor
            }
        }
        get { return "" }
    }
}

@IBDesignable extension UIButton {

    @IBInspectable var textColorIB: String? {
        set {
            if let newColor = UIColor.getColorByName(newValue) {
                self.setTitleColor(newColor, for: .normal)
            }
        }
        get { return "" }
    }
}
