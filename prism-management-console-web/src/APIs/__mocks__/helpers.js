/* eslint no-magic-numbers: "off" */

import { random, image, date as fakeDate } from 'faker';
import moment from 'moment';

export const toProtoDate = date => ({
  year: date.year(),
  month: date.month() + 1, // because Months are zero indexed
  day: date.day()
});

const createMockTransaction = () => ({
  id: random.alphaNumeric(999),
  icon: image.avatar(),
  date: moment(fakeDate.recent()).unix(),
  type: 'Connection'
});

export const createMockTransactions = quantity => {
  const transactions = [];
  for (let i = 0; i < quantity; i++) transactions.push(createMockTransaction());

  return transactions;
};
