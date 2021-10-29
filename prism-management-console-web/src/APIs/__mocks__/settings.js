let SETTINGS = {
  payForCredential: false
};

export const getSettings = () => Promise.resolve(SETTINGS);

export const editSettings = newSettings =>
  new Promise(resolve => {
    SETTINGS = newSettings;
    resolve(200);
  });
