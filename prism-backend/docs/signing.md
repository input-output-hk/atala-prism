## Problems with signing structured data
Signing data can be harder than one would imagine. The main problem is not with the signing algorithm itself but with assuring that the same exact data is used for signing and for signature verification. Just one byte differing will cause verification to fail.

There are two ways of providing signature:
1. Store it externally - for example just appending it to the document, using some kind of separator,
2. Add it as a part of the document itself - this way the signature is computed for version of document **without the signature field appended**; this method is often used for JSON where we want to keep the document structure valid.

The second distinction is on how to obtain the exact byte sequence used for signing in cases when the same value can be represented in many ways:
1. Let signer choose the representation and just use the one provided during signature verification. This approach is problematic if we want to store the signature as part as the signed document: signature has to be removed from it during signing and such process may be ambiguous as well. There are solutions for that problem like signing document with signature field present, but containing some artificial value like zeros string with the length of a proper signature[<sup>1</sup>](#1).
2. Generate canonical representation and use it for signing. Canonizalization can be hard to implement correctly, as edge cases can make rendering difficult[<sup>1</sup>](#1) (e.g. JSON object field ordering when unicode characters are present).
3. Serialize data into bytes in a way completely unrelated to format used for storing it, e.g. using JSON for storage, but some kind of more unambiguous binary format for signing. This can be easier than canonicalization, but has downside of maintaining two serialization formats.

## Common signature schema

If we want to create a signature schema that could support many variants following design can be used:

```
type Enclosure

enclose: T => Enclosure
getSignedBytes: Enclosure => Array[Byte]
compose: (Enclosure, Array[Byte]) => String
decompose: String => (Enclosure, Array[Byte])
disclose: Enclosure => T
```

`enclose` computes representation that allows easy computation of byte string representation used for signing. It also allows composing it with signature. `deenclose` is reverse process.

`getSignedBytes` uses enclosed value to obtain bytes that should be signed.

`compose` returns encoded representation with signature added. `decompose` extracts enclosed value and signature from encoded representation.

Such model is general enough to be used in various encoding options:
* When signature is stored externally, `Enclosure` might be just `Array[Byte]`. `enclose` / `deenclose` are serialization / deserialization operations, `compose` can mean concatenation of serialized value and signature.
* When signature is stored as a part of document and we are relying on representation provided by the signer `Enclosure` might be a byte sequence with a placeholder to insert signature.
* When signature is stored as a part of canonicalized document, `Enclosure` might be intermediate representation which allows easy rendering of canonicalized version, e.g. with keys sorted inside of JSON objects memory representation.

## Footnotes
<a class="anchor" id="1">1. How (not) to sign a JSON object https://latacora.micro.blog/2019/07/24/how-not-to.html</a>
