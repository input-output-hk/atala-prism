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

  get selectedContactIds() {
    return this.selectedContacts.map(c => c[CONTACT_ID_KEY]);
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
    this.selectedContacts = checked ? yield this.api.contactsManager.getAllContacts(filters) : [];
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
    const newContactId = record[CONTACT_ID_KEY];
    this.selectAllCheckboxState = checkboxStates.INDETERMINATE;

    if (selected) {
      // it's important to create new array because Antd has some PureComponent/memo optimizations,
      // so change is not detected
      this.selectedContacts = [...this.selectedContacts, record];
    } else {
      this.selectedContacts = this.selectedContacts.filter(
        contactRow => contactRow[CONTACT_ID_KEY] !== newContactId
      );
    }
  }
}
