//

import AlamofireObjectMapper
import Foundation
import ObjectMapper

class SharedMemoryHelper: NSObject {

    let preferences = UserDefaults.standard

    override init() {}

    func getObject(key: String, internalObject: Any?) -> Any? {

        if internalObject != nil {
            return internalObject
        }
        return preferences.object(forKey: key)
    }

    func getMappableObject<T: Mappable>(key: String, internalObject: T?) -> T? {

        if internalObject != nil {
            return internalObject
        }

        if let jsonString = getObject(key: key, internalObject: internalObject) {
            return T(JSONString: jsonString as! String)
        }
        return nil
    }

    func getMappableArray<T: Mappable>(key: String, internalObject: [T]?) -> [T]? {

        if internalObject != nil {
            return internalObject
        }

        if let jsonString = getObject(key: key, internalObject: internalObject) {
            return [T](JSONString: jsonString as! String)
        }
        return nil
    }

    func setObject(key: String, internalObject: Any?) {

        self.preferences.set(internalObject, forKey: key)
        // self.preferences.synchronize()
    }

    func setMappableObject<T: Mappable>(key: String, internalObject: T?) {
        setObject(key: key, internalObject: internalObject?.toJSONString())
    }

    func setMappableArray<T: Mappable>(key: String, internalObject: [T]?) {
        setObject(key: key, internalObject: internalObject?.toJSONString())
    }
}
