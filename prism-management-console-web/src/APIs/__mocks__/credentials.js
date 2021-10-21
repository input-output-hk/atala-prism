import { date as fakeDate, image, lorem, name as fakeName, random } from 'faker';
import moment from 'moment';
import __ from 'lodash';
import { CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';
import { toProtoDate } from './helpers';

export const credentials = [];
export const categories = [];
export const groups = [];

const createMockStudent = () => ({
  avatar: image.avatar(),
  name: `${fakeName.firstName()} ${fakeName.lastName()}`
});

const createMockCredential = () => ({
  id: random.alphaNumeric(999),
  issuedBy: random.alphaNumeric(999),
  subject: random.alphaNumeric(999),
  icon: image.avatar(),
  title: lorem.words(),
  identityNumber: random.number(100000),
  enrollmentdate: toProtoDate(moment(fakeDate.recent())),
  graduationdate: toProtoDate(moment(fakeDate.recent())),
  groupName: random.alphaNumeric(9),
  student: random.number(5) > 1 ? createMockStudent() : null,
  credential: random.arrayElement(credentials),
  category: random.arrayElement(categories),
  group: random.arrayElement(groups)
});

const mockCredentials = [];

for (let i = 0; i < 3 * CREDENTIAL_PAGE_SIZE; i++) mockCredentials.push(createMockCredential());

const promisify = response => Promise.resolve(response);

export const getCredentials = () =>
  promisify({
    credentials: mockCredentials,
    count: mockCredentials.length
  });

export const getTotalCredentials = () => promisify(mockCredentials.length);

export const getCredentialTypes = () => promisify(credentials);

export const getCategoryTypes = () => promisify(categories);

export const getCredentialsGroups = () => promisify(groups);

export const deleteCredential = ({ id }) => {
  const index = __.findIndex(mockCredentials, ({ groupId }) => groupId === id);

  mockCredentials.splice(index, 1);

  return promisify(200);
};
