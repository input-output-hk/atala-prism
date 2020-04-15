//

class ApiParseUtils: NSObject {

    static func parseDate(_ intDate: Int64) -> String {
        let date = Date(fromMillis: intDate)
        return date.string(withFormat: "MMM dd yyyy")
    }

    static func parseDate(_ intDate: Io_Iohk_Prism_Protos_Date) -> String {
        return "\(intDate.month)/\(intDate.day)/\(intDate.year)"
    }

    static func parseFullName(_ intSubject: Io_Iohk_Prism_Protos_SubjectData) -> String {

        var fullName = ""
        for name in intSubject.names {
            fullName = "\(fullName) \(name)"
        }
        for surname in intSubject.surnames {
            fullName = "\(fullName) \(surname)"
        }
        return fullName.trim()
    }
}
