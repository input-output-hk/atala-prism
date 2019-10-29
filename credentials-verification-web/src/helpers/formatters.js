import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getBrowserLanguage } from './languageUtils';

export const dateFormatter = date => date;

export const completeDateFormatter = date => {
  const lang = getBrowserLanguage();

  moment.locale(lang);

  return moment.unix(date).format('DD/MM/YYYY');
};
