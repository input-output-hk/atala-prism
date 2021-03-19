@@@ index
* @ref:[Preparation](preparation.md)
* @ref:[Generate an identity](generate-identity.md)
* @ref:[Issue Credential](issue-credential.md)
* @ref:[Verify Credential](verify-credential.md)
@@@

# Usage Tutorial

This tutorial walks you through the necessary steps to do the basic operations with the PRISM SDK, you will learn how to generate identities, as well as how to deal with credentials (issuance/verification).

For simplicity reasons, we are using code that doesn't involve any network calls. The only resulting limitation is inability to revoke a credential because that's an event which needs to be posted to the Cardano network.
