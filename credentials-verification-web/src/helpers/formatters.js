import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getBrowserLanguage } from './languageUtils';

const completeDateFormatter = (date, format) => {
  const lang = getBrowserLanguage();
  moment.locale(lang);
  return moment.unix(date).format(format);
};

export const backendDateFormatter = format => date => completeDateFormatter(date, format);
export const frontendDateFormatter = format => date => date.format(format);

export const longDateFormatter = backendDateFormatter('LLLL');
export const shortDateFormatter = backendDateFormatter('lll');

export const monthDayFormat = frontendDateFormatter('MMM DD');
