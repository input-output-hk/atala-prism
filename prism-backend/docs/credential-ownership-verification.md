# Credential Ownership Verification

This document describes a basic option for an owner to prove ownership
of a credential. This is necessary to convince potential verifiers that the 
entity they are interacting with is indeed the owner of the credential being 
used for authentication. We also mention an advanced option that tackles the
main drawback of the basic approach. However, since this requires relevant
research and engineering effort, it is just left as a mention for now. For both
cases (basic and advanced), we list some pros and cons.

We do not impose any specific structure in the credentials, but assume that the 
credential owner has associated a signig key pair with the credential. The 
public key of this pair will need to be "somehow" present in the credential: 
either explicitly by writing it (or a fingerprint of it) in plaintext as a 
credential attribute, or in some sort of obfuscated form.

Of course, the validity of the credential itself should be checked as usual.

The common general approach is for verifier and credential owner to engage in an
interactive protocol. The reason behind requiring an interactive protocol is to 
prevent replay attacks.

## Basic Option

In this case, the public key of the credential owner needs to be present in 
plaintext inside the credential. Having DIDs, this could be something like:

- `holderDID`: Uniquely identifies the holder of the credential.
- `holderSigningKey`: Uniquely identifies the signing key within `holderDID` 
  that is associated to the current credential.
  
In the following, to ease notation, we just refer to the signing key pair as 
`hsk` for holder signing key (i.e., the private part of the key pair), and `hvk` 
for holder verification key (i.e., the public part of the key pair).

Given this, the protocol by means of which credential owner `O` proves
ownership of credential `cred` to verifier `V`, is as follows:

```
1. O -> V: cred
2.      V: r = randomNonce(l)
3. V -> O: r
4. 	    O: s = sign(hsk,r)
5. O -> V: s
6. 	    V: b = verify(hvk,s,r)
```

Where `r` is a value produced uniformly at random of `l` bits.

Having the holder sign a value freshly computed challenge by the verifier 
ensures that, with overwhelming probability, the received response is not a 
replay. For a replay attack to be successful, a collision in the produced nonce 
needs to take place which, with nonces of `l` bits computed uniformly at random,
equals to `1/2^l`. It is also good practice to prepend or append some fixed 
magic value to the challenge, so as to ensure domain separation and prevent 
malicious verifiers to reuse the signature in some other context. Alternatively,
the signed challenge response may also include the credential whose 
ownership is being proven -- while this is not a fixed value, it is probably 
enough to prevent malleability attacks. In this case, given a credential `cred`,
the previous protocol becomes:

```
1. O -> V: cred
2.      V: r = randomNonce(l)
3. V -> O: r
4. 	    O: s = sign(hsk,(r,cred))
5. O -> V: s
6. 	    V: b = verify(hvk,s,(r,cred))
```

This is actually the basic idea behind the *Verifiable Presentations*, where a
presentation is essentially a verifiable credential with an extra top-level
"proof" structure, including a `challenge` field (see, e.g., 
[this example](https://www.w3.org/TR/vc-data-model/#example-a-simple-example-of-a-verifiable-presentation)).

### Pros and Cons of the Basic Option

**Pros**:

- Simplicity: This approach just requires conventional cryptography (digital
  signatures, and a secure random number generator).
- Low (computational) costs: Direct consequence of the previous point.
- Wide availability of suitable cryptography libraries.

**Cons**:

- No privacy: Since the credential owner's public key identifier is explicitly
  written in the credential, and this key is needed for verification of the
  challenge response, all credential presentations are linkable, even across
  verifiers. This has a severe impact in privacy, unless we make the too strong
  assumption that verifiers do not share data.

## Advanced Options

The goal of any advanced option is to address the privacy constraints of the
basic option -- which, despite being only negative aspect, is very relevant. 
There are certainly ways to do it, although they come at a high cost. They 
involve advanced cryptography, and are in any case alternatives that are 
currently being intensively studied in the academic literature, although some
are beginning to gain traction. The complexity stems from the fact that, at the 
cost of improving privacy, we lose efficiency and functionality. Therefore, here
we just mention the main approach so far, based on anonymous credentials.

Anonymous credentials refine verifiable credentials (despite being much older)
so that it is possible to include attributes in the credentials that are never
shared in plaintext with the verifiers. Specifically, this enables the 
credential owner to share a credential, prove that he knows the private key
associated to the (hidden) public key, and reveal (a subset of) the other 
attributes in the credential, or even an arbitrary functin of them. Typically, 
these constructions lie on top of zero-knowledge protocols, as well as other
cryptographic primitives.

### Pros and Cons of the Advanced Options

**Pros**:

- High privacy: Credential presentations are unlinkable to the credential
issuance. Also multiple credential presentations are unlinkable among 
themselves.

**Cons**:

- High computational costs: These protocols usually require heavy-weight 
cryptography, such as zero-knowledge proofs. Thus, computational costs
increase notably with respect to the basic protocols.
- High complexity for supporting extra functionality: Adding privacy usually
comes at the cost of losing utility. For instance, it becomes harder to apply
data mining processes to extract utility from actions by users. Or it becomes
harder to revoke credentials. 
- Potential unavailability of cryptographic libraries.


## Additional Topics

### Preventing Man-In-The-Middle Attacks

The previous protocols assume an authenticated channel between credential
owner and verifier. This is essential. Otherwise, if the channel is not 
authenticated, an adversary can perform a man-in-the-middle attack. For 
instance, in order to authenticate against verifier `V1`, the adversary may 
intercept an authentication of the legitimate holder against another verifier 
`V2`, and replace the nonce aimed to the holder by the one he received from 
`V1`.

How to establish an authenticated communication channel depends on the specific
application scenario. Therefore, we just make here a note for futher 
consideration when needed.

### Key Management

Independently on what option is the chosen one, all security lies on a secure
management of the key pair that the credential owner associates to the 
credential. In this regard, the usual alternatives apply:

- Using secure components in mobile devices or computers (e.g., TEEs, TPMs, 
  etc.).
- Use hardware wallets.
- Use mnemonic-based wallets that restrict exposure.

An aspect to consider when using hardware-based solutions (like TEEs or 
hardware wallets) would be to ensure that no device lock-in takes place. 
That is, if we aim at allowing a credential owner to use his credentials
from any device he owns, hardware components that do not allow exporting
private keys may hinder precisely those use cases.
