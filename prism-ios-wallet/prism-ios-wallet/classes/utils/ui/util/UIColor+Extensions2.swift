// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
import Foundation
import UIKit

extension UIColor {

    /// SwifterSwift: https://github.com/SwifterSwift/SwifterSwift
    /// Hexadecimal value string (read-only).
    public var hexString: String {
        let components: [Int] = {
            let comp = cgColor.components!
            let components = comp.count == 4 ? comp : [comp[0], comp[0], comp[0], comp[1]]
            return components.map { Int($0 * 255.0) }
        }()
        return String(format: "#%02X%02X%02X", components[0], components[1], components[2])
    }

    /// SwifterSwift: https://github.com/SwifterSwift/SwifterSwift
    /// Short hexadecimal value string (read-only, if applicable).
    public var shortHexString: String? {
        let string = hexString.replacingOccurrences(of: "#", with: "")
        let chrs = Array(string)
        guard chrs[0] == chrs[1], chrs[2] == chrs[3], chrs[4] == chrs[5] else { return nil }
        return "#\(chrs[0])\(chrs[2])\(chrs[4])"
    }

    /// Color to Image
    func toImage(size: CGSize = CGSize(width: 1, height: 1)) -> UIImage {
        let rect: CGRect = CGRect(origin: .zero, size: size)
        UIGraphicsBeginImageContextWithOptions(rect.size, true, 0)
        self.setFill()
        UIRectFill(rect)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image! // was image
    }

    /// SwifterSwift: https://github.com/SwifterSwift/SwifterSwift
    /// RGB components for a Color (between 0 and 255).
    ///
    ///        UIColor.red.rgbComponents.red -> 255
    ///        UIColor.green.rgbComponents.green -> 255
    ///        UIColor.blue.rgbComponents.blue -> 255
    ///
    public var rgbComponents: (red: Int, green: Int, blue: Int) {
        var components: [CGFloat] {
            let comp = cgColor.components!
            if comp.count == 4 {
                return comp
            }
            return [comp[0], comp[0], comp[0], comp[1]]
        }
        let rComp = components[0]
        let gComp = components[1]
        let bComp = components[2]
        return (red: Int(rComp * 255.0), green: Int(gComp * 255.0), blue: Int(bComp * 255.0))
    }

    /// SwifterSwift: https://github.com/SwifterSwift/SwifterSwift
    /// RGB components for a Color represented as CGFloat numbers (between 0 and 1)
    ///
    ///        UIColor.red.rgbComponents.red -> 1.0
    ///        UIColor.green.rgbComponents.green -> 1.0
    ///        UIColor.blue.rgbComponents.blue -> 1.0
    ///
    public var cgFloatComponents: (red: CGFloat, green: CGFloat, blue: CGFloat) {
        var components: [CGFloat] {
            let comp = cgColor.components!
            if comp.count == 4 {
                return comp
            }
            return [comp[0], comp[0], comp[0], comp[1]]
        }
        let rComp = components[0]
        let gComp = components[1]
        let bComp = components[2]
        return (red: rComp, green: gComp, blue: bComp)
    }

    /// SwifterSwift: https://github.com/SwifterSwift/SwifterSwift
    /// Get components of hue, saturation, and brightness, and alpha (read-only).
    public var hsbaComponents: (hue: CGFloat, saturation: CGFloat, brightness: CGFloat, alpha: CGFloat) {
        var hComp: CGFloat = 0.0
        var sComp: CGFloat = 0.0
        var bComp: CGFloat = 0.0
        var aComp: CGFloat = 0.0

        self.getHue(&hComp, saturation: &sComp, brightness: &bComp, alpha: &aComp)
        return (hue: hComp, saturation: sComp, brightness: bComp, alpha: aComp)
    }

    /// Random color.
    public static var random: UIColor {
        let rComp = Int(arc4random_uniform(255))
        let gComp = Int(arc4random_uniform(255))
        let bComp = Int(arc4random_uniform(255))
        return UIColor(red: rComp, green: gComp, blue: bComp)
    }
}

// MARK: Initializers

public extension UIColor {

    convenience init(hex: Int, alpha: CGFloat = 1.0) {
        let rComp = CGFloat((hex & 0xFF0000) >> 16) / 255
        let gComp = CGFloat((hex & 0xFF00) >> 8) / 255
        let bComp = CGFloat(hex & 0xFF) / 255
        self.init(red: rComp, green: gComp, blue: bComp, alpha: alpha)
    }

    /**
     Creates an UIColor from HEX String in "#363636" format

     - parameter hexString: HEX String in "#363636" format
     - returns: UIColor from HexString
     */
    convenience init(hexString: String, alpha: CGFloat = 1.0) {

        var hexFormatted = hexString.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines).uppercased()

        if hexFormatted.hasPrefix("#") {
            hexFormatted = String(hexFormatted.dropFirst())
        }

        assert(hexFormatted.count == 6, "Invalid hex code used.")

        var rgbValue: UInt64 = 0
        Scanner(string: hexFormatted).scanHexInt64(&rgbValue)

        self.init(red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
                  green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
                  blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
                  alpha: alpha)
    }

    /// Create UIColor from RGB values with optional transparency.
    ///
    /// - Parameters:
    ///   - red: red component.
    ///   - green: green component.
    ///   - blue: blue component.
    ///   - transparency: optional transparency value (default is 1)
    convenience init(red: Int, green: Int, blue: Int, transparency: CGFloat = 1) {
        assert(red >= 0 && red <= 255, "Invalid red component")
        assert(green >= 0 && green <= 255, "Invalid green component")
        assert(blue >= 0 && blue <= 255, "Invalid blue component")
        var trans: CGFloat {
            if transparency > 1 {
                return 1
            } else if transparency < 0 {
                return 0
            } else {
                return transparency
            }
        }
        self.init(red: CGFloat(red) / 255.0, green: CGFloat(green) / 255.0, blue: CGFloat(blue) / 255.0, alpha: trans)
    }
}
