import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const EN = require('./languages/en.i18n.json');
const KA = require('./languages/ka.i18n.json');

const en = 'en';
const ka = 'ka';

const DEFAULT_LANG = en;

const langs = [en, ka];

const browserLang = window.navigator.language || DEFAULT_LANG;
const preferredLang = browserLang.substr(0, 2);

const langToUse = langs.includes(preferredLang) ? preferredLang : DEFAULT_LANG;

const i18nInitialise = () =>
  i18n.use(initReactI18next).init({
    lng: langToUse,
    resources: {
      en: {
        translation: EN
      },
      ka: {
        translation: KA
      }
    },
    fallbackLng: langToUse
  });

export default i18nInitialise;
