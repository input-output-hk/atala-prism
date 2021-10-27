import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getBrowserLanguage, saveLang } from './helpers/languageUtils';

const EN = require('./languages/en.i18n.json');
const KA = require('./languages/ka.i18n.json');

const i18nInitialise = () => {
  const defaultLang = getBrowserLanguage();

  saveLang(defaultLang);

  return i18n.use(initReactI18next).init({
    lng: defaultLang,
    resources: {
      en: {
        translation: EN
      },
      ka: {
        translation: KA
      }
    },
    fallbackLng: defaultLang
  });
};

export default i18nInitialise;
