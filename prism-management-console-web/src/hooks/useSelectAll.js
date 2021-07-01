const { MAX_CONTACTS } = require('../helpers/constants');

export const useSelectAllContacts = (contactsManager, setSelected) => {
  const handleSelectAll = async e => {
    if (e.target.checked) {
      const list = await contactsManager.getContacts(null, MAX_CONTACTS);
      setSelected(list.map(contact => contact.contactId));
    } else setSelected([]);
  };

  return handleSelectAll;
};

export const useSelectAllGroups = (groupsManager, setSelected) => {
  const handleSelectAll = async e => {
    if (e.target.checked) {
      const list = await groupsManager.getAllGroups();
      setSelected(list.map(group => group.name));
    } else setSelected([]);
  };

  return handleSelectAll;
};
