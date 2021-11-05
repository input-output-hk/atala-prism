/* eslint no-magic-numbers: "off" */
import { internet, name, random, date } from 'faker';
import moment from 'moment';
import { PENDING_CONNECTION, CONNECTED, DEFAULT_PAGE_SIZE } from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { createMockTransactions, toProtoDate } from './helpers';

const createMockHolder = () => ({
  avatar: internet.avatar(),
  name: `${name.firstName()} ${name.lastName()}`,
  identityNumber: random.number(),
  admissionDate: toProtoDate(moment(date.recent())),
  email: internet.email(),
  status: random.arrayElement([PENDING_CONNECTION, CONNECTED]),
  id: random.alphaNumeric(999),
  transactions: createMockTransactions(random.number(7) + 1)
});

const mockedHolders = [];

for (let i = 0; i < 3 * DEFAULT_PAGE_SIZE; i++) mockedHolders.push(createMockHolder());

export const getHolders = ({
  identityNumber = 0,
  name: filterName = '',
  status = '',
  pageSize,
  offset
}) =>
  new Promise(resolve => {
    const filteredHolders = mockedHolders.filter(
      ({ identityNumber: holderIdentityNumber, status: holderStatus, name: holderName }) =>
        (!status || status === holderStatus) &&
        holderName.toLowerCase().includes(filterName.toLowerCase()) &&
        holderIdentityNumber.toString().includes(identityNumber.toString())
    );

    const skip = offset * pageSize;
    resolve({
      holders: filteredHolders.slice(skip, skip + pageSize),
      holderCount: filteredHolders.length
    });
  });

export const inviteHolder = ({ id }) => {
  Logger.info('Invited the user with the id: ', id);
  return Promise.resolve(200);
};
