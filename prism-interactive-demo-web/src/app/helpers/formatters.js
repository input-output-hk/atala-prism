import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getBrowserLanguage } from './languageUtils';

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
