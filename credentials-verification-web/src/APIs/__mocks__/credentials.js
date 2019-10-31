import { date as fakeDate, image, lorem, name as fakeName, random } from 'faker';
import moment from 'moment';
import { CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';
import {
  filterByExactMatch,
  filterByInclusion,
  filterByNewerDate
} from '../../helpers/filterHelpers';

export const credentials = ['CREDENTIAL1', 'CREDENTIAL2', 'CREDENTIAL3'];
export const categories = ['CATEGORY1', 'CATEGORY2', 'CATEGORY3'];
export const groups = ['GROUP1', 'GROUP2', 'GROUP3'];

const createMockStudent = () => ({
  avatar: image.avatar(),
  name: `${fakeName.firstName()} ${fakeName.lastName()}`
});

const createMockCredential = () => ({
  id: random.alphaNumeric(999),
  icon: image.avatar(),
  name: lorem.words(),
  identityNumber: random.number(100000),
  admissionDate: moment(fakeDate.recent()).unix(),
  groupId: random.alphaNumeric(9),
  student: random.number(5) > 1 ? createMockStudent() : null,
  credential: random.arrayElement(credentials),
  category: random.arrayElement(categories),
  group: random.arrayElement(groups)
});

const mockCredentials = [];

for (let i = 0; i < 3 * CREDENTIAL_PAGE_SIZE; i++) mockCredentials.push(createMockCredential());

export const getCredentials = ({
  credentialId,
  name: filterName,
  date = 0,
  credentialType,
  category: categoryFilter,
  group: groupFilter,
  offset
}) =>
  new Promise(resolve => {
    const filteredCredentials = mockCredentials.filter(
      ({ id, name, admissionDate, credential, category, group }) =>
        filterByInclusion(credentialId, id) &&
        filterByInclusion(filterName, name) &&
        filterByNewerDate(date, admissionDate) &&
        filterByExactMatch(credentialType, credential) &&
        filterByExactMatch(categoryFilter, category) &&
        filterByExactMatch(groupFilter, group)
    );

    const skip = offset * CREDENTIAL_PAGE_SIZE;
    resolve({
      credentials: filteredCredentials.slice(skip, skip + CREDENTIAL_PAGE_SIZE),
      count: filteredCredentials.length
    });
  });

export const getTotalCredentials = () => new Promise(resolve => resolve(mockCredentials.length));

export const getCredentialTypes = () => new Promise(resolve => resolve(credentials));

export const getCategoryTypes = () => new Promise(resolve => resolve(categories));

export const getCredentialsGroups = () => new Promise(resolve => resolve(groups));
