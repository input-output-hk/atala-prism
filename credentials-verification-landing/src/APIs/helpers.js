export const setDateInfoFromJSON = (date, { year, month, day }) => {
  date.setYear(year);
  date.setMonth(month);
  date.setDay(day);
};
