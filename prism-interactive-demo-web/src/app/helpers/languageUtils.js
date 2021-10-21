export const getBrowserLanguage = () => {
  const en = 'en';
  const ka = 'ka';

  const DEFAULT_LANG = en;
  const langs = [en, ka];

  // In case the user has IE the language is in userLanguage, otherwise that
  // value doesn't exist and therefore tries to get it from language
  const browserLang = DEFAULT_LANG;

  const preferredLang = browserLang.substr(0, 2);

  return langs.includes(preferredLang) ? preferredLang : DEFAULT_LANG;
};
