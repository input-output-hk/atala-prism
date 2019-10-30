export const filterByInclusion = (filter, field) =>
  !filter || field.toLowerCase().includes(filter.toLowerCase());

export const filterByExactMatch = (filter, field) => !filter || filter === field;

export const filterByNewerDate = (filter, field) => !filter || filter < field;
