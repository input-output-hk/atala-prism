---
title: PRISM Slayer Protocol Specification
---

# Intro

This is a description of PRISM's 2nd layer protocol, internally called "Slayer".
Historically, it has evolved in three variations (v1, v2, v3), which are
provided here as the following three sections. Each variation has its own
document, so we simply concatenate the three documents in order to create the
current one. The original markdown source can be found in the
[repository](https://github.com/input-output-hk/atala/tree/develop/prism-backend/docs/protocol).

In Slayer v1, the reference to Bitcoin is there because Bitcoin has been our
initial target blockchain. Since then, we have transitioned to Cardano.
Currently there is a tension between keeping Slayer's original
blockchain-agnostic nature versus tying it to Cardano by leveraging its more
flexible metadata feature.

The final sections of this document describe canonicalization, key derivation,
unpublished DIDs, the _late publication_ attack, and some more ideas on evolving
the protocol.

This work is **CONFIDENTIAL** and &copy; [IOHK](https://iohk.io). The technology
we describe here powers [atalaprism.io](https://atalaprism.io) and the
respective web and mobile applications.
