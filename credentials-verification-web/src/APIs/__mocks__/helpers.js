export const toProtoDate = date => ({
  year: date.year(),
  month: date.month() + 1, // because Months are zero indexed
  day: date.day()
});
