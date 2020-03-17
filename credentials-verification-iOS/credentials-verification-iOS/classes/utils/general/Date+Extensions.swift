//

extension Formatter {

    static let iso8601: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()
}

extension Date {

    init(fromMillis: Int64?) {
        self = Date(timeIntervalSince1970: TimeInterval((fromMillis ?? 0) / 1000))
    }

    // Note: ISO8601 string format: yyyy-MM-dd'T'HH:mm:ss.SSSZ.
    var iso8601: String {
        return Formatter.iso8601.string(from: self)
    }

    private static func isYesterday(date: Date) -> Bool {
        return NSCalendar.current.isDateInYesterday(date)
    }

    // Generic

    // Returns the amount of years from another date
    func years(from date: Date) -> Int {
        return Calendar.current.dateComponents([.year], from: date, to: self).year ?? 0
    }

    // Returns the amount of months from another date
    func months(from date: Date) -> Int {
        return Calendar.current.dateComponents([.month], from: date, to: self).month ?? 0
    }

    // Returns the amount of weeks from another date
    func weeks(from date: Date) -> Int {
        return Calendar.current.dateComponents([.weekOfYear], from: date, to: self).weekOfYear ?? 0
    }

    // Returns the amount of days from another date
    func days(from date: Date) -> Int {
        return Calendar.current.dateComponents([.day], from: date, to: self).day ?? 0
    }

    // Returns the amount of hours from another date
    func hours(from date: Date) -> Int {
        return Calendar.current.dateComponents([.hour], from: date, to: self).hour ?? 0
    }

    // Returns the amount of minutes from another date
    func minutes(from date: Date) -> Int {
        return Calendar.current.dateComponents([.minute], from: date, to: self).minute ?? 0
    }

    // Returns the amount of seconds from another date
    func seconds(from date: Date) -> Int {
        return Calendar.current.dateComponents([.second], from: date, to: self).second ?? 0
    }

    // Returns the a custom time interval description from another date
    func offset(from date: Date) -> String {
        if years(from: date) > 0 { return "\(years(from: date))y" }
        if months(from: date) > 0 { return "\(months(from: date))M" }
        if weeks(from: date) > 0 { return "\(weeks(from: date))w" }
        if days(from: date) > 0 { return "\(days(from: date))d" }
        if hours(from: date) > 0 { return "\(hours(from: date))h" }
        if minutes(from: date) > 0 { return "\(minutes(from: date))m" }
        if seconds(from: date) > 0 { return "\(seconds(from: date))s" }
        return ""
    }

    // Returns a String with the format input as String
    func string(withFormat format: String) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = format
        return dateFormatter.string(from: self)
    }

    func dateString() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        return dateFormatter.string(from: self)
    }

    func displayableString() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MM-dd-yyyy"
        return dateFormatter.string(from: self)
    }

    func displayableSlashedString() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MM/dd/yyyy"
        return dateFormatter.string(from: self)
    }

    func timeString() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "hh:mm a"
        dateFormatter.amSymbol = "AM"
        dateFormatter.pmSymbol = "PM"
        return dateFormatter.string(from: self)
    }

    init(simpleDate: String) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        self = dateFormatter.date(from: simpleDate) ?? Date()
    }

    init(fullDate: String) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        self = dateFormatter.date(from: fullDate) ?? Date()
    }

    static func displayableRelativeString(fullDate: String?) -> String? {

        guard let dateStr = fullDate else {
            return nil
        }
        let date = Date(fullDate: dateStr)

        // Setup the relative formatter
        let relDF = DateFormatter()
        relDF.doesRelativeDateFormatting = true
        relDF.dateStyle = .long
        relDF.timeStyle = .short

        // Setup the non-relative formatter
        let absDF = DateFormatter()
        absDF.dateStyle = .long
        absDF.timeStyle = .short

        // Get the result of both formatters
        let rel = relDF.string(from: date)
        let abs = absDF.string(from: date)

        // If the results are the same then it isn't a relative date.
        // Use your custom formatter. If different, return the relative result.
        if rel == abs {
            let fullDF = DateFormatter()
            fullDF.dateFormat = "dd MMMM 'at' hh:mm a"
            return fullDF.string(from: date)
        } else {
            return rel
        }
    }
}
