package io.iohk.cef.data

//Wrapper class used before signing a delete. It encloses the intention of the owner within the signature
case class DeleteSignatureWrapper[I](dataItem: DataItem[I])
