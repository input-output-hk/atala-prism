//

class ApiService: NSObject {

    static let global = ApiService()
    let sharedMemory = SharedMemory.global
}
