let SETTINGS = {
  payForCredential: false
};

const EDIT_DELAY_MS = 200;

export const getSettings = () => Promise.resolve(SETTINGS);

export const editSettings = newSettings =>
  new Promise(resolve => {
    SETTINGS = newSettings;
    resolve(EDIT_DELAY_MS);
  });
