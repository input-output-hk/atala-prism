import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getBrowserLanguage } from './helpers/languageUtils';

const EN = require('./languages/en.i18n.json');
const KA = require('./languages/ka.i18n.json');

const i18nInitialise = () =>
  i18n.use(initReactI18next).init({
    lng: getBrowserLanguage(),
    resources: {
      en: {
        translation: EN
      },
      ka: {
        translation: KA
      }
    },
    fallbackLng: getBrowserLanguage()
  });

export default i18nInitialise;
