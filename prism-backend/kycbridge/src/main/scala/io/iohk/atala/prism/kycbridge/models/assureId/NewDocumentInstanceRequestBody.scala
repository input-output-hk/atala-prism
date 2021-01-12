package io.iohk.atala.prism.kycbridge.models.assureId

/**
  * This request model contains only required fields determined by trial and error method. Documentation at
  * https://services.assureid.net/AssureIDService/help/operations/PostDocumentInstance
  * is not accurate or updated. For example: device field in Request is described as minOccurs="0" and nillable="true"
  * yet this field is required. (same for manufacture and model of DeviceType)
  */
case class NewDocumentInstanceRequestBody(
    device: Device,
    subscriptionId: String
)

case class Device(
    `type`: DeviceType
)

case class DeviceType(
    manufacturer: String,
    model: String
)
