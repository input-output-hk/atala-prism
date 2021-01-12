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

    static func saveLogo(key: String, logo: Data) {

        if SharedMemory.global.imageBank == nil {
            SharedMemory.global.imageBank = ImageBank()
        }

        SharedMemory.global.imageBank?.logos?[key] = logo

        SharedMemory.global.imageBank = SharedMemory.global.imageBank
    }
}
