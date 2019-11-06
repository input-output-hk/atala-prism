import moment from 'moment';
import { image, name as fakeName, random, date as fakeDate } from 'faker';
import { CREDENTIAL_SUMMARY_PAGE_SIZE } from '../../helpers/constants';

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
  transactions: createMockTransactions(random.number(7) + 1)
});

const createMockCredentialSummary = () => ({
  id: random.alphaNumeric(999),
  user: createMockUser(),
  icon: image.avatar(),
  date: moment(fakeDate.recent()).unix()
});

const credentialSummaries = [];

for (let i = 0; i < 3 * CREDENTIAL_SUMMARY_PAGE_SIZE; i++)
  credentialSummaries.push(createMockCredentialSummary());

export const getCredentialSummaries = ({ name: filterName, date: filterDate, offset }) =>
  new Promise(resolve => {
    const filteredCredentialSummaries = credentialSummaries.filter(
      ({ user: { name }, date }) =>
        (!filterName || name.toLowerCase().includes(filterName.toLowerCase())) &&
        (!filterDate || date > filterDate)
    );

    const skip = offset * CREDENTIAL_SUMMARY_PAGE_SIZE;

    resolve({
      credentialSummaries: filteredCredentialSummaries.slice(
        skip,
        skip + CREDENTIAL_SUMMARY_PAGE_SIZE
      ),
      credentialSummariesCount: filteredCredentialSummaries.length
    });
  });
