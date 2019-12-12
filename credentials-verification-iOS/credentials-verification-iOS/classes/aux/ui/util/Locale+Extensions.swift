// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
import Foundation

extension Locale {

    static func locale(forCountry countryName: String) -> String? {
        return Locale.isoRegionCodes.filter { self.countryName(fromLocaleCode: $0) == countryName }.first
    }

    static func countryName(fromLocaleCode localeCode: String) -> String {
        return (Locale(identifier: "en_UK") as NSLocale).displayName(forKey: .countryCode, value: localeCode) ?? ""
    }
}
