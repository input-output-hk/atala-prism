import faker from 'faker';
import moment from 'moment';
import { PENDING_CONNECTION, CONNECTED, PENDING_INVITATION } from '../../helpers/constants';
import Logger from '../../helpers/Logger';

const mockedHolders = [
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_CONNECTION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: CONNECTED,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  },
  {
    avatar: faker.internet.avatar(),
    name: `${faker.name.firstName()} ${faker.name.lastName()}`,
    identityNumber: faker.random.number(),
    admissionDate: moment(faker.date.recent()).unix(),
    email: faker.internet.email(),
    status: PENDING_INVITATION,
    id: faker.random.alphaNumeric(9999)
  }
];

export const getHolders = ({ userId = '', name = '', status = '', pageSize, offset }) =>
  new Promise(resolve => {
    const filteredHolders = mockedHolders.filter(
      ({ id, status: holderStatus, name: holderName }) =>
        (!status || status === holderStatus) &&
        holderName.toLowerCase().includes(name.toLowerCase()) &&
        id.toLowerCase().includes(userId.toLowerCase())
    );

    const skip = offset * pageSize;
    resolve({
      holders: filteredHolders.slice(skip, skip + pageSize),
      holderCount: filteredHolders.length
    });
  });

export const inviteHolder = ({ id }) => {
  Logger.info('Invited the user with the id: ', id);
  return new Promise(resolve => resolve(200));
};
