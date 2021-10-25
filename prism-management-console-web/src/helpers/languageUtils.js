import { getI18n } from 'react-i18next';
import { DEFAULT_LANGUAGE } from './constants';

const EN = 'en';
const KA = 'ka';

export const saveLang = defaultLang => localStorage.setItem(DEFAULT_LANGUAGE, defaultLang);

const getSavedLang = () => localStorage.getItem(DEFAULT_LANGUAGE);

export const getLanguages = () => {
  const i18nObject = getI18n();

  if (!i18nObject) return [EN, KA];

  return Object.keys(i18nObject.options.resources);
};

export const getCurrentLanguage = () => (getI18n() ? getI18n().language : EN);

export const changeLanguage = newLanguage => {
  if (newLanguage === getCurrentLanguage()) return;

  getI18n().changeLanguage(newLanguage);
  saveLang(newLanguage);
};

export const getBrowserLanguage = () => {
  const savedLang = getSavedLang();

  if (savedLang) return savedLang;

  const langs = getLanguages();
  const [DEFAULT_LANG] = langs;

  // In case the user has IE the language is in userLanguage, otherwise that
  // value doesn't exist and therefore tries to get it from language
  const browserLang = window.navigator.userLanguage || window.navigator.language || DEFAULT_LANG;

  const preferredLang = browserLang.substr(0, 2);

  return langs.includes(preferredLang) ? preferredLang : DEFAULT_LANG;
};
