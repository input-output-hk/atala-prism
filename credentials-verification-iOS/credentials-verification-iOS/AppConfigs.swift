//
import Foundation

struct AppConfigs {

    // Changed the statusbar color of all the app's ViewControllers
    static var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }

    // Values
    static let CORNER_RADIUS_REGULAR: CGFloat = 10.0
    static let CORNER_RADIUS_BUTTON: CGFloat = 22.0
    static let TABLE_HEADER_HEIGHT_REGULAR: CGFloat = 15.0
}

extension UIColor {

    static let appWhite = UIColor(netHex: 0xFFFFFF)
    static let appBlackLight = UIColor(netHex: 0x202020)
    static let appBlack = UIColor(netHex: 0x000000)
    static let appRed = UIColor(netHex: 0xFF2D3B)
    static let appRedLight = UIColor(netHex: 0xFFEAEB)
    static let appGreen = UIColor(netHex: 0x1ED69E)
    static let appGreen15per = UIColor(netHex: 0x2600_D793)
    static let appGrey = UIColor(netHex: 0xB6B6B6)
    static let appGreySub = UIColor(netHex: 0x979797)
    static let appGreyBlue = UIColor(netHex: 0xA0AABB)
    static let appGreyMid = UIColor(netHex: 0xE5E5E5)
    static let appGreyLight = UIColor(netHex: 0xF8F9F9)
    static let appViolet = UIColor(netHex: 0xAF2DFF)
    static let appBlue = UIColor(netHex: 0x4A2DFF)
    static let appPropertyGrey = UIColor(netHex: 0x3C393A)

    static let ColorNameDic: [String: UIColor] = [
        "appWhite": UIColor.appWhite,
        "appBlackLight": UIColor.appBlackLight,
        "appBlack": UIColor.appBlack,
        "appRed": UIColor.appRed,
        "appRedLight": UIColor.appRedLight,
        "appGreen": UIColor.appGreen,
        "appGreen15per": UIColor.appGreen15per,
        "appGrey": UIColor.appGrey,
        "appGreySub": UIColor.appGreySub,
        "appGreyBlue": UIColor.appGreyBlue,
        "appGreyMid": UIColor.appGreyMid,
        "appGreyLight": UIColor.appGreyLight,
        "appViolet": UIColor.appViolet,
        "appBlue": UIColor.appBlue,
        "appPropertyGrey": UIColor.appPropertyGrey,
    ]
}
