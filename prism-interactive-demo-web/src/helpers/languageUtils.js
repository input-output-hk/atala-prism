export const getBrowserLanguage = () => {
  const en = 'en';
  const ka = 'ka';

  const DEFAULT_LANG = en;
  const langs = [en, ka];

  // In case the user has IE the language is in userLanguage, otherwise that
  // value doesn't exist and therefore tries to get it from language
  const browserLang = window.navigator.userLanguage || window.navigator.language || DEFAULT_LANG;

  const preferredLang = browserLang.substr(0, 2);

  const langToUse = langs.includes(preferredLang) ? preferredLang : DEFAULT_LANG;

  return langToUse;
};
