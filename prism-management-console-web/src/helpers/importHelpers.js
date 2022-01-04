const blankContact = {
  externalId: '',
  contactName: ''
};

export const createBlankContact = key => ({
  ...blankContact,
  key
});
