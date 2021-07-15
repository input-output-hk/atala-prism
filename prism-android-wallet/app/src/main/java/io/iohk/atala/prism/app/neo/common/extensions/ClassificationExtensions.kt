package io.iohk.atala.prism.app.neo.common.extensions

import com.acuant.acuantdocumentprocessing.model.Classification

fun Classification.isBackSideRequired(): Boolean {
    (type.supportedImages as? ArrayList<HashMap<*, *>>)?.let { list ->
        list.forEach { map ->
            if (map.containsKey("Light") && map.containsKey("Side") && map["Light"] == 0 && map["Side"] == 1) {
                return true
            }
        }
    }
    return false
}
