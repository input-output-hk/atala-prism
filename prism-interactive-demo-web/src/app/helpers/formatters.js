import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getBrowserLanguage } from './languageUtils';

const completeDateFormatter = (date, format) => {
  const lang = getBrowserLanguage();
  return moment
    .utc()
    .year(date.year)
    .month(date.month - 1) // because Months are zero indexed
    .date(date.day)
    .locale(lang)
    .format(format);
};

const completeFrontendDateFormatter = (date, format) => {
  const lang = getBrowserLanguage();
  moment.locale(lang);
  // The dates come from the backend as unix timestamp with miliseconds
  return moment.unix(date / 1000).format(format);
};

export const toProtoDate = date => {
  const lang = getBrowserLanguage();
  const dateAsNumberArray = moment(date)
    .locale(lang)
    .format('L')
    .split('/')
    .map(Number);

  let year;
  let month;
  let day;

  if (lang === 'en') {
    [month, day, year] = dateAsNumberArray;
  } else {
    [day, month, year] = dateAsNumberArray;
  }

  return {
    year,
    month,
    day
  };
};

export const backendDateFormatter = format => date => completeDateFormatter(date, format);
export const frontendDateFormatter = format => date => completeFrontendDateFormatter(date, format);

export const longDateFormatter = frontendDateFormatter('LLLL');
export const shortDateFormatter = frontendDateFormatter('lll');
export const shortBackendDateFormatter = backendDateFormatter('lll');
export const dayMonthYearFormatter = frontendDateFormatter('L');
export const dayMonthYearBackendFormatter = backendDateFormatter('L');

export const monthDayFormat = frontendDateFormatter('MMM DD');

export const dateAsUnix = date => (date ? moment(date).unix() : 0);
