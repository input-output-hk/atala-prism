import { image, name as fakeName, date as fakeDate, random } from 'faker';
import moment from 'moment';
import { PAYMENT_PAGE_SIZE } from '../../helpers/constants';

// GEL is short for lari, georgian money
const CURRENCIES = ['USD', 'GEL'];

const createMockUser = () => ({
  icon: image.avatar(),
  name: `${fakeName.firstName()} ${fakeName.lastName()}`
});

const createMockPayment = () => ({
  user: createMockUser(),
  date: moment(fakeDate.recent()).unix(),
  amount: random.number(300) * 100,
  currency: random.arrayElement(CURRENCIES)
});

const mockPayments = [];

for (let i = 0; i < PAYMENT_PAGE_SIZE * 3; i++) mockPayments.push(createMockPayment());

const promisify = response => new Promise(resolve => resolve(response));

export const getCurrencies = () => {
  const currencies = mockPayments.map(({ currency }) => currency);

  const uniqueCurrencies = [...new Set(currencies)];

  return promisify(uniqueCurrencies);
};

export const getAmounts = () => {
  const amounts = mockPayments.map(({ amount }) => amount);

  const uniqueAmounts = [...new Set(amounts)];

  return promisify(uniqueAmounts);
};

const sortPayments = (filteredPayments, { field, order }) => {
  const compare =
    order === 'ascend' ? (first, second) => first > second : (first, second) => first < second;

  const getValue = payment => (field === 'name' ? payment.user.name : payment[field]);

  return filteredPayments.sort((firstElement, secondElement) =>
    compare(getValue(firstElement), getValue(secondElement)) ? 1 : -1
  );
};

export const getPayments = ({ from, to, payer, offset = 0, sort }) => {
  const filteredPayments = mockPayments.filter(
    ({ user: { name }, date }) =>
      (!payer || name.toLowerCase().includes(payer.toLowerCase())) &&
      (!from || from < date) &&
      (!to || to < date)
  );

  const sortedPayments = sort ? sortPayments(filteredPayments, sort) : filteredPayments;

  const skip = offset * PAYMENT_PAGE_SIZE;

  return promisify({
    payments: sortedPayments.slice(skip, skip + PAYMENT_PAGE_SIZE),
    paymentCount: sortedPayments.length
  });
};
