/*eslint no-magic-numbers: "off"*/
import __ from 'lodash';
import icon from '../../images/icon-groups.svg';

// This group  is hardcoded as it was discussed for the alpha verion
const mockGroup = {
  icon,
  groupName: 'Finance Class of 2019',
  groupId: 1,
  lastUpdate: {
    year: 2018,
    month: 5,
    day: 25
  }
};

const mockedGroups = [mockGroup];

export const getGroups = ({ name = '', date: filterDate = 0, pageSize, lastId }) =>
  new Promise(resolve => {
    const filteredGroups = mockedGroups.filter(
      ({ groupId, groupName, lastUpdate }) =>
        (!lastId || groupId > lastId) &&
        (!name || groupName.toLowerCase().includes(name.toLowerCase())) &&
        (!filterDate || lastUpdate > filterDate)
    );

    resolve({
      groupsList: filteredGroups.slice(0, pageSize),
      totalNumberOfGroups: filteredGroups.length
    });
  });

export const deleteGroup = ({ id = '' }) =>
  new Promise((resolve, reject) => {
    const index = __.findIndex(mockedGroups, ({ groupId }) => groupId === id);

    if (index === -1) reject(new Error('500'));

    mockedGroups.splice(index, 1);

    resolve(200);
  });
