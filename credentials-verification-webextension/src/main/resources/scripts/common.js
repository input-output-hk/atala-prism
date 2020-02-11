/**
 * Expose functions that are difficult to represent in Scala.
 *
 * The idea is to create a facade that simplifies them to our needs.
 */
var facade = {
  notify: (title, message, iconUrl) => {
    chrome.notifications.create("", { title: title, message: message, type: "basic", iconUrl: iconUrl })
  },
  setBadgeText: (text, callback) => {
    chrome.browserAction.setBadgeText({text: text}, callback)
  },
  setBadgeBackgroundColor: (color, callback) => {
    chrome.browserAction.setBadgeBackgroundColor({color: color}, callback)
  },
  setPopup: (popup, callback) => {
    chrome.browserAction.setPopup({popup: popup}, callback)
  }
};
