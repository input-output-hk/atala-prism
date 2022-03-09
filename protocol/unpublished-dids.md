# Unpublished DIDs

## Objective

In order to increase the scalability and reduce operational costs for our [protocol](./protocol-v0.3.md), we have 
proposed the use of unpublished DIDs. In the current implementation (at the time of this writing), every DID is 
published on-chain as soon as it is created. After publication, a node can resolve a DID and obtain the current state of
the DID document. Publishing a DID currently allows to:

- Resolve the DID
- Send DID update operations
- Issue credentials in our setting where proofs of existence are posted on-chain

The main drawback we face is that every DID publication requires an underlying ledger transaction, leading to a delay
between the moment the DID is generated, and the time it becomes resolvable by nodes. The process also adds a fee cost.
We find it reasonable to believe that a substantial number of users won't use every DID for all the above features. 
Hence, we propose to mitigate the stated drawbacks by allowing a DID to fulfil a subset of functionalities _before_
being published. In particular, we would like to create a concept of **unpublished DID** that: 

- Allows a DID to Be resolvable before on-chain publication 
- Allows the DID to be published if needed

In the rest of the document, we present an approach we have evaluated from Sidetree and comment on the potential next 
steps we could follow.


## Approaches reviewed

### Sidetree

In [Sidetree](https://identity.foundation/sidetree/spec/) we can see the definition of 
[Long form DID URI](https://identity.foundation/sidetree/spec/#long-form-did-uris) designed to

> * Resolving the DID Documents of unpublished DIDs.
> * Authenticating with unpublished DIDs.
> * Signing and verifying credentials signed against unpublished DIDs.
> * After publication and propagation are complete, authenticating with either the Short-Form DID URI or Long-Form DID URI.
> * After publication and propagation are complete, signing and verifying credentials signed against either the Short-Form DID URI or Long-Form DID URI.

In a simplified description, the specification uses URI parameters (the `?initial-state=` parameter) to attach the 
initial state of the DID to the DID URI. The intended use is that, a resolver would take the DID and attempt to resolve
it, if the associated DID Document is not found, then the resolver returns the decoded initial state document attached 
in the `initial-state` URI parameter. If, however, the DID Document is resolved, then the `initial-state` value is 
ignored and resolution works as if the URI parameter was never attached.

The approach described faced the following problem
As discussed in issues [#782](https://github.com/decentralized-identity/sidetree/issues/782) and 
[#777](https://github.com/decentralized-identity/sidetree/issues/777) on Sidetree's repository and on
[this issue](https://github.com/w3c/did-core/issues/337) in DID Core, the use of URI parameters may lead to some
inconsistencies in the resolved DID document. For example, DID Documents 
[MUST have an `id` field](https://w3c.github.io/did-core/#did-subject) which MUST be a DID. This means that if we have
a long form DID: 
  
  ```
   did:<method>:<suffix>?initial-state=<encoded-state>
  ```
   
which hasn't been published, and we resolve it, the DID Document obtained should look like:
   
  ```
  {
    id: "did:<method>:<suffix>",
    ...
  } 
  ```

leading to an `id` that could be accidentally used and share while being it not resolvable (because the DID could 
remain unpublished). At the same time, the `id` `"did:<method>:<suffix>?initial-state=<encoded-state>"` is not a valid
DID (it is a [DID URL](https://w3c.github.io/did-core/#did-url-syntax) though), leading to an invalid DID Document.

To mitigate the issue, Sidetree's working group added to their reference implementation a 
[different format](https://github.com/decentralized-identity/sidetree/commit/b6945a9286d053e8254b604c8926ce35dc21d47c#diff-e4b25c798093490e1f72b171527e6596R54),

> // Long-form can be in the form of:
> // 'did:<methodName>:<unique-portion>?-<methodName>-initial-state=<create-operation-suffix-data>.<create-operation-delta>' or
> // 'did:<methodName>:<unique-portion>:<create-operation-suffix-data>.<create-operation-delta>'

During the WG call on Tuesday 29th 2020, it was confirmed that the syntax with `-initial-state` will be deprecated.
According to [W3C spec](https://w3c.github.io/did-core/#did-syntax), the second syntax leads to a valid DID.
The long form still maintains a short form counterpart, i.e. for a long form DID:

```
'did:<methodName>:<unique-portion>:<create-operation-suffix-data>.<create-operation-delta>'
```

we have a short form (a.k.a canonical form)

```
'did:<methodName>:<unique-portion>
```
 
The WG didn't define yet (by the time of this writing) how to treat the returned `id` in the resolved DID Document 
_after_ the DID is published. This is, after publication, if a user attempts to resolve the long form DID, should the
`id` field of the resolved document contain the long form or short form of the DID? Different proposals are under 
discussion.

## Proposal for first iteration

In order to provide our equivalent to long form DIDs, we will adopt the form:

```
 did:prism:hash(<initial-state>):base64URL(<initial-DID-Document>)
```

### Impact on recovery process

We must consider the impact of these new DIDs in our [recovery process](./key-derivation.md#did-recovery).
The problem we have with unpublished DIDs, is that they won't be found on-chain during the recovery process iteration.
this will lead the algorithm to stop in step 4.1. Given that mobile apps will be the first users of these DIDs, it looks
reasonable to adapt the DID recovery process to ask the connector if a DID was used for a connection instead of asking
 or DID resolution. In the future, we could store all generated DID Documents in a data vault like service.

### DID length

We decided to make some tests to measure how long our long form DIDs would look like.
We generated 100000 DIDs under 3 settings:

1. With 1 master key
2. With 2 master keys
3. With 3 master keys

The code used can be found [here](../../node/test/src/io/iohk/atala/prism/node/poc/EncodedSizes.scala).
Example results are:

For 1 key
```
Generating 100000 dids

printing 3 shortest DIDs
(did:prism:7e5b03ee503c6d63ca814c64b1e6affd3e16d5eafb086ac667ae63b44ea23f2b:CmAKXhJcCgdtYXN0ZXIwEAFCTwoJc2VjcDI1NmsxEiA4BHOH6oJYfe_pVSqXl69HcRW304Mk4ExOwJye9VWcGBogOBL32BWDzjqnSqoQss8x_qv_02fwyrEL13QCKT9KRoQ=,207)
(did:prism:8f4ecd2b7123b0ddfb7c4a443b3d25823041d470b388e6bb0577cd5d49f9b8a9:CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiEA-NGsBmlol3-5qMZ6BXp1Al6-24NAORgjEA1RBhHiYAIaIEDrXK8kjANIzJmOLVHFtKq0j7H_l8Uj96UlyDO3qjk7,207)
(did:prism:0ad343d0c4b9a69014cde239589df57fb3a0adbe0fb3f5c8c4a294bbfd9827cb:CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiEA-BxLxhOrKhum0RqhdN-eLCnyMtXHlyYu6b1ASOpvxb4aICf-6F4DzlCthmI0FDomlzbp_GHo_5zFfpUk9c24Jpfe,207)

printing 3 longest DIDs
(did:prism:0dcbbc7ba0e4194bc43fee20cb9ecf156d9c191b2eb88e75a45decf68d56665c:CmIKYBJeCgdtYXN0ZXIwEAFCUQoJc2VjcDI1NmsxEiEA55RbxHRgZ1zB3rSpHjYuclIF_9G7yhGgWGi7fc6JxVIaIQCEu6hOdE-JljVGcrW5zgendY3fZtDpbJJbNsS2Tjb32Q==,211)
(did:prism:bbf07987129f43ea213d7c1a1222d6a242efe76c9c853c27d7c90560f287c10d:CmIKYBJeCgdtYXN0ZXIwEAFCUQoJc2VjcDI1NmsxEiEA5_5gl70kwcDvl5gKH2_AJzry1cT2XZHSBfS1dmQTIY0aIQDiK3AHz9ilWDgqWXOFCbyIMlDgYWh9C5-NTni6EYwdjA==,211)
(did:prism:8b5094925066ade140f99c9bba00c0a8f139882138b5939180af4d827388c1f8:CmIKYBJeCgdtYXN0ZXIwEAFCUQoJc2VjcDI1NmsxEiEA6NKGFL-n5dek0qQP-Qlwq-ea6TatJNYgCNY-ysRfyzYaIQCDs0VVxTcvWn9tZxZufla7dIMzQsSU-Jg_fU3j5XWJNQ==,211)

Average DID length 207.99972 bytes
```

For 2 keys
```
Generating 100000 dids

printing 3 shortest DIDs
(did:prism:8159135db4b3cac4c09667087ea7c46b1019d85885f72af2bab083ccd092c5d4:Cr0BCroBEloKB21hc3RlcjAQAUJNCglzZWNwMjU2azESHj2JQyzK59GApMJoM5O28nQho9tpIOonINJRUWIE1hogbnHhV4pZQM0eAMBUQ0MC6g0Sa9uF87G74kmN6DvBFpQSXAoHbWFzdGVyMRABQk8KCXNlY3AyNTZrMRIgI9WfWp31w4eQOOEQF2mzVZCednBNIYugJQwL42wkrQYaIFR2BcIwYSnmXEBVbVaYmfz6DOcPPDEgSfVZOK-2kgFE,331)
(did:prism:471d8c0b8b813de92b4a70604a56fc0ea68d84ccd9c1a4de9ebaeb1afb906235:CsABCr0BElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIEQ0OMfxIlt-qpR7lPGNy2G2SldPY8_NPm3ndKkDaCt6GiAirtTWn0QeztWE5i-ZwENrS6PNoAXXFBom1jTAYoxD6hJdCgdtYXN0ZXIxEAFCUAoJc2VjcDI1NmsxEiEAtP0Rm24Fn2Mg6S67SMlBCvoZ09LgKZvwpf26TlxCj7caIH4n2UvHcNYIY6k7EDBtgyDptgpcgR5EWxaf-pLO_PJ-,335)
(did:prism:ef51a3fa3e5e222c819575e3f137ad45c1c1db7e3c06b21a590332e73d20953b:CsABCr0BEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIQDE_vSfeNUmoyV-qm2jJYVTqTMQ8T83X8IsWBLMt3ctERogNRgDXpxUyteC2kBCEB-L7SIEt7ivJyBETjJcuUHX1IgSXAoHbWFzdGVyMRABQk8KCXNlY3AyNTZrMRIgMu7Gocu-ESEtzbgXNT2tpZzH_UqRC-8KoFPu1fNUscYaIDiDhA1LVhJYHWazwJpGZjB3lQ1kIy-VrO7rRDVQ3zKv,335)

printing 3 longest DIDs
(did:prism:6c5ca0c67ea138eb41ee1a8060527b8d3bbf921e103e977b77786f6b71567000:CsIBCr8BEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIQCHJNU9sXhHZy3EdgzfGjzClA0B9PVJbe2apsG0ljhdqRogJnESFoQd9AjqBgIcaKmwyCYnVm6tHucyAOcCq59BtfcSXgoHbWFzdGVyMRABQlEKCXNlY3AyNTZrMRIhAJqTQ7CzDZ8d02kXFFlw_Do5sEiKpZdtRQLAw9EqhEi9GiEAtMlOZRfGivpnfpDbn7uDBZTb5Rj3Y3rTgSfIdAFhQP0=,339)
(did:prism:0a3d98977ebeda7d11bddcc98d3b3e7072886e3e91ddf2fa7f99bdb1c110b00a:CsIBCr8BEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIFWdJTyU0lf-lbiLKxcn2Ags4V88I0QSZash5WRm7UBUGiEAnaq_ipQkUUYXUaYe9WzUqdFa6LOv6tOIPH4Rfm4vmhQSXgoHbWFzdGVyMRABQlEKCXNlY3AyNTZrMRIhAMdis1_dwKgyV02uZQCfvx0O9J-dXkP1DXZUbiiA9kVvGiEAtShw9wGDMe1FnO1Be7MANPknlhUlZAPsZMbf7YT6C2E=,339)
(did:prism:ca6b2058009b455dca1c3b491fb06e4d37713eda32099b2cf91e31e06befb569:CsIBCr8BEl4KB21hc3RlcjAQAUJRCglzZWNwMjU2azESIQDS7RopaBAO6dq3l9TBD_x-FhHAvH68wszcHKDSWguUUBohAO0eUWnWHTHNVXoqsHYwfYABRZ_wlCVTfcZKsV-ZuSd_El0KB21hc3RlcjEQAUJQCglzZWNwMjU2azESIEqn3DsW7NdGHD96OOIMXUTzl0ab3m5v4oaWZHqqbjNVGiEA3kk0XVALFtm-U7zp1oWUaAP-y4fJlUVoUCDiE6h1OEU=,339)

Average DID length 337.737 bytes
```

For 3 keys
```
Generating 100000 dids

printing 3 shortest DIDs
(did:prism:e2f9e03bf49c5644066dd6efb09a130576c46e4c687861c18685ffc0c63f0bce:Cp0CCpoCEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIFgi7BOQhH8RtgZist8Ag416pL_JYBfaP0RsO21FI2JaGiEA2bzA4iPnrvX-8cdbVjytOfafp48MGuJiSMqhKYenM70SWwoHbWFzdGVyMRABQk4KCXNlY3AyNTZrMRIgbHxvyJdH5S_0ZhI4L2sh_hEdrixpAJ3eE3lpHfh-bYYaH12aRPGE3QntYeekBH24cfwLmbb-dVCByILWvWtXMwMSXAoHbWFzdGVyMhABQk8KCXNlY3AyNTZrMRIgeVpCGozZ_KhkwPd46HJnkcqmdDckGVJHLJgyuxjvh5saIHt3BeIApG6jqj84ZP66AfsTUpuNQP1BSVLzdhcQLoPD,459)
(did:prism:ad7a81274c0a0295dcf4417ccc501ccfbca1165bf2bcad94430aa42171ed173d:Cp0CCpoCElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIBWIwCfSj6MtNG29Z_t7-AJRWyd1w95efBnf5BhA_3QMGiBLq18HV9KGL--BusOyqeWInFu98A0lVFLUGK4snwus9hJcCgdtYXN0ZXIxEAFCTwoJc2VjcDI1NmsxEiBwLUQ1jTnE8JdPcf_C41tyd7Y2loCkGlykfslZy0g4ShogUco4yJ-vf4vhCV3BATY33tI5ybM6y1y8L8VHP4BtnAASXAoHbWFzdGVyMhABQk8KCXNlY3AyNTZrMRIgAY_UwVb48SGeOxspuFTqEYWedQOCL_6lyd198ABWTfcaIBifedeOY8K3CKAZf06S1BG3fGodNzN3jAfi5oV1usmI,459)
(did:prism:b7800057dd8d48c86709db5f9191105666301449fa481f0bd4e2e0f244af2745:Cp0CCpoCElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIGrZ5TSsDsQCUVwUs0wIe-wCkh-VSFgDfAsP06qVCYZZGiBEztwdpOi71HeyYVkowJt70_moGYxHe-5L8cT0WTcmCxJcCgdtYXN0ZXIxEAFCTwoJc2VjcDI1NmsxEiA8B_NKd1_1G_eGFO5SPDjg3Am0xWQKX7opBwVV5XU02hogenHpXX30EfTwbZCQhyLNRMaZDQFcfJk5t6f1V1c0v1wSXAoHbWFzdGVyMhABQk8KCXNlY3AyNTZrMRIgGMcERNmpRhx04j39poUvu1TMVpnSVjvoaez5WW3oKEMaICRim4pqzKYerpkkxmalZAC_NSA-rngcnUkzsiw3-NeD,459)

printing 3 longest DIDs
(did:prism:4bb94e65c2bc37fc4e52c4f2471418302d26e43cf126677f407a0451be9a7c22:CqECCp4CEl4KB21hc3RlcjAQAUJRCglzZWNwMjU2azESIQCdzPQA1RFTIpEp1CJNwO0wqe1qz5JawnI8sDlZfrkw5BohANF1qt5111DzBKPYsnrSjm01zJ--_470DQopP4QRyGgHEl0KB21hc3RlcjEQAUJQCglzZWNwMjU2azESIQC_IY74aciuAQDyLAZjXGwaq1QDfHd27okxcGXm6OOVUxogEhbKgQWBZTWzRrjLkpAg4c-TiCtOXK7rOVvoFkV8FnYSXQoHbWFzdGVyMhABQlAKCXNlY3AyNTZrMRIgXm2tdR9ReKjOJi2H3YtvQPkaSO06zkFjAKR_WDHYzJMaIQDUI8-MVdBoinpUUy0zRX8aUlhZ1ij4nwDBfJB6QnJrow==,467)
(did:prism:ca7fbf765201acafc4e20ce38ef4dc3d159b6067b5b13ee99ffbf6b7e3f6facc:CqECCp4CEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIAqGJ2KqLJ3rfQ-hkB2eKRSLgfqcN314tB1F0P6gM-6kGiEAohZpSVfv_ZYiMQv2IosQdgZjEzNlzzwBeGDpnXJ01lYSXgoHbWFzdGVyMRABQlEKCXNlY3AyNTZrMRIhAKyAvzd6NmegIuPYsQKU6brHjjrkEoTyuCWfui7vQZ05GiEA8yogy0Ex_MbjWq_a6U-J97bekXrEzGzcqKhOh3CjxMcSXQoHbWFzdGVyMhABQlAKCXNlY3AyNTZrMRIhAIcBqfZKEmKSudx6j4D_g_V4UMwFmRsBGQ1ncX6s3l1vGiAhXUWh5fgnfAFnZhGHh6rElvI8XaVfI6gBbdT2UriRDw==,467)
(did:prism:33afe0e357fa60eaf205ec9f1fd3e579c3a656c27b5143fbbdfa35c2c2b5916d:CqECCp4CEl0KB21hc3RlcjAQAUJQCglzZWNwMjU2azESIQCwxcy1xkqJcbETa07ucq1GrlEfmCSj9S6U5PwuDOb58BogIzMlA9N-1sRDsRSwpsUrC755bmfNQyok-6tWa2q-cT8SXgoHbWFzdGVyMRABQlEKCXNlY3AyNTZrMRIhALwExYd3DQbo37G8MvF3E-0q-dvR5CG_mkGr11WtNd3_GiEAh7kMetZcM7RgMrnru4An2K2ECP8QvCl6_0TjslU8S9gSXQoHbWFzdGVyMhABQlAKCXNlY3AyNTZrMRIgRF_1TeGjfM64opVJOQfOPbqoldGQgdw-ZiE3X5-TqIoaIQC7Epdgj-kYey_E1RqnIO_slSU29uTXjhDjw-N0tMKT5A==,467)

Average DID length 464.28944 bytes
```

We see an increase of ~130 bytes per key. We should point out that we are representing keys as elliptic curve points. 
We could reduce space by representing keys in compressed representation (i.e. a coordinate and a byte that indicates the
sign of the other coordinate).

By comparison, we can see [this Sidetree test vector](https://github.com/decentralized-identity/sidetree/blob/master/tests/fixtures/longFormDid/longFormDid.txt)
that represents a long for DID of them. The DID has 883 bytes. 
