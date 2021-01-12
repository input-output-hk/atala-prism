let SETTINGS = {
  payForCredential: false
};

export const getSettings = () => new Promise(resolve => resolve(SETTINGS));

export const editSettings = newSettings =>
  new Promise(resolve => {
    SETTINGS = newSettings;
    resolve(200);
  });
