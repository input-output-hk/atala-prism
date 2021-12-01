import { makeAutoObservable } from 'mobx';
import { CONTACT_ID_KEY } from '../../helpers/constants';

const checkboxStates = {
  UNCHECKED: 'UNCHECKED',
  CHECKED: 'CHECKED',
  INDETERMINATE: 'INDETERMINATE'
};

export default class ContactsSelectStore {
  selectedContacts = [];

  isLoadingSelection = false;

  selectAllCheckboxState = checkboxStates.UNCHECKED;

  constructor(api) {
    this.api = api;

    makeAutoObservable(
      this,
      {
        api: false
      },
      { autoBind: true }
    );
  }

  get selectAllCheckboxStateProps() {
    return {
      checked: this.selectAllCheckboxState === checkboxStates.CHECKED,
      indeterminate: this.selectAllCheckboxState === checkboxStates.INDETERMINATE
    };
  }

  *selectAllContacts(ev, filters) {
    this.isLoadingSelection = true;
    const { checked } = ev.target;
    this.selectAllCheckboxState = checked ? checkboxStates.CHECKED : checkboxStates.UNCHECKED;
    const entitiesToSelect = checked ? yield this.api.contactsManager.getAllContacts(filters) : [];
    this.selectedContacts = entitiesToSelect.map(e => e[CONTACT_ID_KEY]);
    this.isLoadingSelection = false;
  }

  resetSelection() {
    this.selectedContacts = [];
    this.selectAllCheckboxState = checkboxStates.UNCHECKED;
    this.isLoadingSelection = false;
  }

  /**
   *  Use this as a onSelect handler in Antd Table
   * @param {Object} record - selected row's data
   * @param {boolean} selected
   */
  handleCherryPickSelection(record, selected) {
    const contactId = record[CONTACT_ID_KEY];
    this.selectAllCheckboxState = checkboxStates.INDETERMINATE;

    if (selected) {
      this.selectedContacts.push(contactId);
    } else {
      this.selectedContacts = this.selectedContacts.filter(scId => scId !== contactId);
    }
  }
}
