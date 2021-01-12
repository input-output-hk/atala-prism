//

struct SimpleLocalizedError: LocalizedError {

    var title: String?
    var code: Int
    var errorDescription: String? { return _description }
    var failureReason: String? { return _description }

    private var _description: String

    init(title: String? = nil, _ description: String, code: Int = 0, doLocalize: Bool = true) {
        self.title = title == nil ? "error_generic_title".localize() : (doLocalize ? title?.localize() : title)
        self._description = doLocalize ? description.localize() : description
        self.code = code
    }
}
