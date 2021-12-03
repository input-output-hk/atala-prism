import { makeAutoObservable } from 'mobx';
import { GROUP_ID_KEY } from '../../helpers/constants';

const checkboxStates = {
  UNCHECKED: 'UNCHECKED',
  CHECKED: 'CHECKED',
  INDETERMINATE: 'INDETERMINATE'
};

export default class GroupsSelectStore {
  selectedGroupsObjects = [];

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

  get selectedGroups() {
    return this.selectedGroupsObjects.map(groupRow => groupRow[GROUP_ID_KEY]);
  }

  get selectedGroupsNames() {
    return this.selectedGroupsObjects.map(groupRow => groupRow.name);
  }

  get selectAllCheckboxStateProps() {
    return {
      checked: this.selectAllCheckboxState === checkboxStates.CHECKED,
      indeterminate: this.selectAllCheckboxState === checkboxStates.INDETERMINATE
    };
  }

  *selectAllGroups(ev, filters) {
    this.isLoadingSelection = true;
    const { checked } = ev.target;
    this.selectAllCheckboxState = checked ? checkboxStates.CHECKED : checkboxStates.UNCHECKED;
    this.selectedGroupsObjects = checked ? yield this.api.groupsManager.getAllGroups(filters) : [];
    this.isLoadingSelection = false;
  }

  resetSelection() {
    this.selectedGroupsObjects = [];
    this.selectAllCheckboxState = checkboxStates.UNCHECKED;
    this.isLoadingSelection = false;
  }

  /**
   *  Use this as a onSelect handler in Antd Table
   * @param {Object} record - selected row's data
   * @param {boolean} selected
   */
  handleCherryPickSelection(record, selected) {
    const newGroupId = record[GROUP_ID_KEY];
    this.selectAllCheckboxState = checkboxStates.INDETERMINATE;

    if (selected) {
      // it's important to create new array because Antd has some PureComponent/memo optimizations,
      // so change is not detected
      this.selectedGroupsObjects = [...this.selectedGroupsObjects, record];
    } else {
      this.selectedGroupsObjects = this.selectedGroupsObjects.filter(
        groupRow => groupRow[GROUP_ID_KEY] !== newGroupId
      );
    }
  }
}
