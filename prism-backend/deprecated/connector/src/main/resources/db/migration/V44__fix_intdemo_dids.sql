UPDATE participants
SET did = 'did:prism:' || encode(sha256(did :: bytea), 'hex');
