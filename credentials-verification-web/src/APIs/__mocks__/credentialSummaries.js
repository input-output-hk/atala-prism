import moment from 'moment';
import { image, name as fakeName, random, date as fakeDate } from 'faker';
import { CREDENTIAL_SUMMARY_PAGE_SIZE } from '../../helpers/constants';
import { createMockTransactions, toProtoDate } from './helpers';

const createMockUser = () => ({
  icon: image.avatar(),
  name: `${fakeName.firstName()} ${fakeName.lastName()}`
});

const createMockCredentialSummary = id => ({
  id,
  user: createMockUser(),
  icon: image.avatar(),
  date: toProtoDate(moment(fakeDate.recent())),
  transactions: createMockTransactions(random.number(7) + 1)
});

const credentialSummaries = [];

for (let i = 0; i < 3 * CREDENTIAL_SUMMARY_PAGE_SIZE; i++)
  credentialSummaries.push(createMockCredentialSummary(i));

export const getCredentialSummaries = ({ name: filterName, date: filterDate, lastId }) =>
  new Promise(resolve => {
    const filteredCredentialSummaries = credentialSummaries.filter(
      ({ user: { name }, date, id }) =>
        (!lastId || id > lastId) &&
        (!filterName || name.toLowerCase().includes(filterName.toLowerCase())) &&
        (!filterDate || date > filterDate)
    );

    resolve(filteredCredentialSummaries.slice(0, CREDENTIAL_SUMMARY_PAGE_SIZE));
  });
