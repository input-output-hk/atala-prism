//

class ApiParseUtils: NSObject {

    static func parseDate(_ intDate: Int64) -> String {
        let date = Date(fromMillis: intDate)
        return date.string(withFormat: "MMM dd yyyy")
    }

    static func parseDate(_ intDate: Io_Iohk_Atala_Prism_Protos_Date) -> String {
        return "\(intDate.month)/\(intDate.day)/\(intDate.year)"
    }

}
