//

extension Array where Element: Equatable {

    // Remove first collection element that is equal to the given `object`:
    mutating func remove(object: Element) {

        if let index = firstIndex(of: object) {
            remove(at: index)
        }
    }

    func containsAll(array: [Element]) -> Bool {

        for item in array {
            if !self.contains(item) {
                return false
            }
        }
        return true
    }
}

extension Array {

    func size() -> Int {
        return count
    }
}
