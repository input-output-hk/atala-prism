export const filterByInclusion = (filter, field) =>
  !filter || field.toLowerCase().includes(filter.toLowerCase());

export const filterByExactMatch = (filter, field) => !filter || filter === field;

export const filterByNewerDate = (filter, field) => !filter || filter < field;

export const filterByManyFields = (toFilter, filterValue, keys) =>
  toFilter.filter(item =>
    keys.reduce((matches, key) => matches || filterByInclusion(filterValue, item[key]), false)
  );
