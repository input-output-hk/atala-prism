import { image, company, random, lorem, date } from 'faker';
import moment from 'moment';
import __ from 'lodash';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';

const createMockGroup = () => ({
  icon: image.avatar(),
  groupName: company.companyName(),
  groupId: random.alphaNumeric(999),
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
  lastUpdate: moment(date.recent()).unix()
});

const mockedGroups = [];

for (let i = 0; i < 3 * GROUP_PAGE_SIZE; i++) mockedGroups.push(createMockGroup());

export const getGroups = ({ name = '', date: filterDate = 0, offset, pageSize }) =>
  new Promise(resolve => {
    const filteredGroups = mockedGroups.filter(
      ({ groupName, lastUpdate }) =>
        (!groupName || groupName.toLowerCase().includes(name.toLowerCase())) &&
        (!filterDate || lastUpdate > filterDate)
    );

    const skip = offset * pageSize;

    resolve({
      groups: filteredGroups.slice(skip, skip + pageSize),
      groupsCount: filteredGroups.length
    });
  });

export const deleteGroup = ({ id = '' }) =>
  new Promise((resolve, reject) => {
    const index = __.findIndex(mockedGroups, ({ groupId }) => groupId === id);

    if (index === -1) reject(new Error(500));

    mockedGroups.splice(index, 1);

    resolve(200);
  });
