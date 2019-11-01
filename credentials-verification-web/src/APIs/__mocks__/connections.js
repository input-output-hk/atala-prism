import moment from 'moment';
import { image, name as fakeName, random, date as fakeDate } from 'faker';
import { CONNECTION_PAGE_SIZE } from '../../helpers/constants';

const createMockTransaction = () => ({
  id: random.alphaNumeric(999),
  icon: image.avatar(),
  date: moment(fakeDate.recent()).unix(),
  type: 'Connection'
});

const createMockTransactions = quantity => {
  const transactions = [];
  for (let i = 0; i < quantity; i++) transactions.push(createMockTransaction());

  return transactions;
};

const createMockUser = () => ({
  icon: image.avatar(),
  name: `${fakeName.firstName()} ${fakeName.lastName()}`,
  transactions: createMockTransactions(random.number(7))
});

const createMockConnection = () => ({
  id: random.alphaNumeric(999),
  user: createMockUser(),
  icon: image.avatar(),
  date: moment(fakeDate.recent()).unix()
});

const connections = [];

for (let i = 0; i < 3 * CONNECTION_PAGE_SIZE; i++) connections.push(createMockConnection());

export const getConnections = ({ name: filterName, date: filterDate, offset }) =>
  new Promise((resolve, reject) => {
    const filteredConnections = connections.filter(
      ({ user: { name }, date }) =>
        (!filterName || name.toLowerCase().includes(filterName.toLowerCase())) &&
        (!filterDate || date > filterDate)
    );

    const skip = offset * CONNECTION_PAGE_SIZE;

    resolve({
      connections: filteredConnections.slice(skip, skip + CONNECTION_PAGE_SIZE),
      connectionsCount: filteredConnections.length
    });
  });
