//
import ObjectMapper

class ImageBank: Mappable {

    var logos: [String: Data]?

    init() {
        logos = [:]
    }

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        logos <- map["logos"]
    }

    // Access helpers

    func logo(for id: String?) -> Data? {
        return id == nil ? nil : logos?[id!]
    }

    static func saveLogos(list: [ConnectionBase]?) {

        if SharedMemory.global.imageBank == nil {
            SharedMemory.global.imageBank = ImageBank()
        }
        for elem in list ?? [] {
            if let id = elem.connectionId, let logo = elem.logoData {
                SharedMemory.global.imageBank?.logos?[id] = logo
            }
        }
        SharedMemory.global.imageBank = SharedMemory.global.imageBank
    }
}
