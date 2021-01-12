# PRISM Wallet Security Analysis

This document details the how the PRISM Wallet works, its known security issues, and the possible mitigation to those.

There are some commits fixing security issues:
- f73e1ceee80ae1a56bd146884957de1100916135

## How it works
The PRISM Wallet is a browser extension (Chrome right now) supporting decentralized identities (DIDs) and verifiable credentials (VCs).

The wallet works like a hardware wallet (Ledger/Trezor), which means that it operates by generating private keys that never leave the wallet, for example, if you need to use a private key for any operation (like decrypting data or issuing a digital signature), the operation is done in the wallet itself.

The wallet has another security mechanism, any sensitive operation needs to be manually reviewed and approved by the wallet owner (like issuing a credential, rotating a key, etc), hence, if a malicious script interacts with the wallet, it won't be able to get important data signed/decrypted because the user is expected to reject such malicious requests.

As PRISM isn't a crypto currency, the wallet doesn't provide any value isolated, hence, it needs to be able to interact with the world, being other websites for now.

The wallet ships a JavaScript SDK that allows any website to communicate with the wallet.


## How extensions work
A browser extension has several different contexts, each context has its own life cycle and privileges, and some of them run in a sandbox.

I'm listing the ones being used by the wallet, as well as how we use them.


### Background
The background context is the one with most privileges, it has access to the full extensions API, it starts when the browser starts, and stays running until the browser stops

It runs in a sandbox and the only way to communicate with it is by messages which it may decide to not respond, by default, some contexts have access to `chrome.runtime.sendMessage` which is the API to send a message to the extension background.

Here the data is stored on the `storage.local` (similar to `localStorage`), but, its encrypted before storing it, these are the details:
- [AES-CTR](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Counter_(CTR)) algorithm, 256 bits on key length.
- [PBKDF2](https://en.wikipedia.org/wiki/PBKDF2) based on user password, uses SHA-512 with a hardcoded salt, 100 iterations.
- Uses the JavaScript [elliptic](https://www.npmjs.com/package/elliptic) library, version `6.5.2`.

Potential issues:
- A session mechanism is required, so that if the wallet owner manually approves an operation from a website, other websites can't exploit that. This could require to verify that the sender website matches the given session, the website is a value filled by the browser that attackers can't hijack.
- The hardcoded salt on the key derivation.
- Some persons ([example](https://crypto.stackexchange.com/questions/6029/aes-cbc-mode-or-aes-ctr-mode-recommended)) on the internet claim that AES EAX or GCM modes are more adequate than CTR, as CTR is simple to misuse.
- External JS dependencies.
- A potential malicious website could DoS the wallet by flooding it with garbage requests, a mitigation could be to allow whitelisting websites that are allowed to interact with it, where only the first request is shown until manual approal from the owner.

### Browser Action (Pop-up)
Commonly extensions add an icon to the browser toolbar, that's the Browser Action context, it starts every time you click on such icon, which commonly displays a popup view, and stops once the popup view gets closed.

This popup view runs in a sandbox, and acts like the hardware wallet screens, whatever is displayed shouldn't be able to be manipulated from the outside (unless we are vulnerable to XSS).

The plan is that whenever a user needs to sign any payload, it will be added to a request queue, which is going to be displayed in the popup, so that the owner can authorize/deny to sign such operations.

The popup can communicate with the background by the `chrome.runtime.sendMessage` API, which is the way to retrieve the request queue, that's because the web sites aren't able to talk directly to the popup which is closed by default.

Potential issues:
- External JS dependencies, its difficult to get rid of those as we have a user interface.
- As we'll display data coming from the outside, we need to sanitize the data to make sure we don't introduce XSS, [DOMPurify](https://www.npmjs.com/package/dompurify) is an option.


### Website Content Script
When building an extension, we can specify that a content-script is injected into some websites, this allows extensions to update the web sites to add functionality, change the UI, etc.

This context starts when the website loads, and stops when the website is closed, it runs in an environment isolated from the website JavaScript but the DOM is shared, which means:
- The JS running on this context and the JS running on the website can't interact.
- All the DOM and its updates are visible to the JS running on the website and the JS running on this context.
- The JS running on this context can update the website DOM to include a `<script src="...">` tag that could load more JS on the website which runs on the website context.

This context can communicate with the background to perform privileged actions, like accessing the storage, uses the same mechanism as the Browser Action (`chrome.runtime.sendMessage`), which means, we get the same potential security issues:
- External JavaScript libraries.

When launched, our wallet SDK gets injected into the website by adding a `<script src="...">` tag.

A plan for this context is to be able to inject HTML on the website to display notifications, ideally, instead of displaying the natively, expose an API on the SDK, so that each website chooses how to display them.

Also, the way to allow any website to communicate with the extension, is to use the Content-Script as a bridge, we expose less functionality than what the background context exposes.

Specifically, we subscribe to the window messages by calling `window.addEventListener("message", eventHandler, useCapture = true)`.

This messaging system doesn't allow to reply to a message, hence, we emulate a request/response mechanism by including a UUID on every request, this context processes the requests and eventually replies by calling the `window.postMessage(msg, origin)` API, the origin ensures that only the requester website gets the message.

New potential issues:
- Not essentially security but injecting minified scripts with `<script src="...">` may collide with the minified scripts from the website.
- When building the notifications view, we need to make sure the website JavaScript can't update it, which could get the user believing that the content is trusted while its not, a potential solution is to embed the view in an iframe.
- Any other website/extension seeing a request sent to the extension could try to DoS our wallet by replying with a message that includes the same request UUID.


### Website SDK
The SDK that runs on the website context doesn't have any privilege as the extensions, it's just like another JS file included in the website. It has the built-in functionality to communicate with the Content-Script.

The way it contacts the Content-Script is by sending a message (`window.postMessage(msg, dom.window.location.origin)`), setting the website origin prevents other websites from grabbing the message BUT it doesn't prevent other JS code or Content-Scripts from different extensions to grab it.

As specified above, matching a response to a request is done by using the `window.addEventListener("message", eventHandler, useCapture = true)` API, which allows other websites to DoS our SDK by sending garbage matching the same request UUID.

Potential issues:
- Any malicious JavaScript/extension embedded in the website could grab any data provided in the interaction with our wallet, there is nothing we can do but advice to use an isolated Chrome profile, if the user is able to see data, the malicious JS can grab it.


## More
Checking the message origin seems to be a common problem, even the React devtools had it: https://github.com/facebook/react-devtools/pull/561

Some resources about security on extensions worth reading:
- https://labs.detectify.com/2016/12/08/the-pitfalls-of-postmessage/
- https://duo.com/labs/tech-notes/message-passing-and-security-considerations-in-chrome-extensions
- https://owasp.org/www-pdf-archive/OWASPLondon_PostMessage_Security_in_Chrome_Extensions.pdf
- https://extensionworkshop.com/documentation/develop/build-a-secure-extension/
