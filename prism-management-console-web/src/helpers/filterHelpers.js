export const filterByInclusion = (filter, field) =>
  !filter || field.toLowerCase().includes(filter.toLowerCase());

export const filterByExactMatch = (filter, field) => !filter || filter === field;

// filters by a series of keys:
// where only the items that include the `filterValue` on any of it's keys will return true
export const filterByMultipleKeys = (filterValue, item, keys) =>
  keys.reduce(
    (partiallyMatches, key) => partiallyMatches || filterByInclusion(filterValue, item[key]),
    false
  );

export const exactValueExists = (list, filter, key) => list.some(item => item[key] === filter);
