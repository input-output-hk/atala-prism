import { image, company, random, lorem, date } from 'faker';
import moment from 'moment';
import __ from 'lodash';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';
import { toProtoDate } from './helpers';

const createMockGroup = groupId => ({
  icon: image.avatar(),
  groupName: company.companyName(),
  groupId,
  certificate: {
    certificateName: lorem.sentence(),
    certificateId: random.alphaNumeric()
  },
  credential: {
    credentialName: lorem.sentence(),
    credentialId: random.alphaNumeric()
  },
  websiteLink: '/',
  description: lorem.paragraph(),
  lastUpdate: toProtoDate(moment(date.recent()))
});

const mockedGroups = [];

for (let i = 0; i < 3 * GROUP_PAGE_SIZE; i++) mockedGroups.push(createMockGroup(i));

export const getGroups = ({ name = '', date: filterDate = 0, pageSize, lastId }) =>
  new Promise(resolve => {
    const filteredGroups = mockedGroups.filter(
      ({ groupId, groupName, lastUpdate }) =>
        (!lastId || groupId > lastId) &&
        (!name || groupName.toLowerCase().includes(name.toLowerCase())) &&
        (!filterDate || lastUpdate > filterDate)
    );

    resolve(filteredGroups.slice(0, pageSize));
  });

export const deleteGroup = ({ id = '' }) =>
  new Promise((resolve, reject) => {
    const index = __.findIndex(mockedGroups, ({ groupId }) => groupId === id);

    if (index === -1) reject(new Error(500));

    mockedGroups.splice(index, 1);

    resolve(200);
  });
