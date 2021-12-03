import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import GroupFilters from '../../../groups/Molecules/Filters/GroupFilters';
import SelectAllButton from './SelectAllButton';
import { useCreateCredentialPageStore } from '../../../../hooks/useCreateCredentialPageStore';
import './_style.scss';

const GroupsTableHeader = observer(({ shouldSelectRecipients, toggleShouldSelectRecipients }) => {
  const { t } = useTranslation();

  const {
    selectedGroups,
    groupsSelectAllCheckboxStateProps,
    isLoadingGroupsSelection,
    selectAllGroups,
    groups,
    groupsFilterSortingProps
  } = useCreateCredentialPageStore();

  const selectAllCheckboxProps = {
    checked: groupsSelectAllCheckboxStateProps.checked,
    indeterminate: groupsSelectAllCheckboxStateProps.indeterminate,
    disabled: !shouldSelectRecipients || !groups.length,
    onChange: selectAllGroups
  };

  return (
    <div className="RecipientsSelectionTableHeader">
      <GroupFilters filterSortingProps={groupsFilterSortingProps} showFullFilter={false} />
      <SelectAllButton
        isLoadingSelection={isLoadingGroupsSelection}
        selectedEntities={selectedGroups}
        checkboxProps={selectAllCheckboxProps}
      />
      <div className="RecipientSelectionCheckbox NoRecipientsCheckbox">
        <Checkbox
          className="CheckboxReverse"
          onChange={toggleShouldSelectRecipients}
          checked={!shouldSelectRecipients}
        >
          <WarningOutlined className="icon" /> {t('newCredential.targetsSelection.checkbox')}
        </Checkbox>
      </div>
    </div>
  );
});

GroupsTableHeader.propTypes = {
  shouldSelectRecipients: PropTypes.bool.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired
};

export default GroupsTableHeader;
