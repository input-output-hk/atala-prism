import { PENDING_CONNECTION, CONNECTED, CONNECTION_STATUSES } from './constants';
import { dateFormat } from './formatters';

export const filterByInclusion = (filter, field) =>
  !filter || field.toLowerCase().includes(filter.toLowerCase());

export const filterByExactMatch = (filter, field) => !filter || filter === field;

export const filterByNewerDate = (filter, field) => !filter || filter < field;

export const filterByUnixDate = (filter, field) => !filter || filter === dateFormat(field);

export const filterByManyFields = (toFilter, filterValue, keys) =>
  toFilter.filter(item =>
    keys.reduce((matches, key) => matches || filterByInclusion(filterValue, item[key]), false)
  );

export const exactValueExists = (list, filter, key) => list.some(item => item[key] === filter);

export const filterContactByStatus = (statusFilter, contactStatus) => {
  switch (statusFilter) {
    case PENDING_CONNECTION:
      return contactStatus < CONNECTION_STATUSES.statusConnectionAccepted;
    case CONNECTED:
      return contactStatus === CONNECTION_STATUSES.statusConnectionAccepted;
    default:
      return true;
  }
};
