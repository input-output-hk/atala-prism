import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import GroupFilters from '../../../groups/Molecules/Filters/GroupFilters';
import SelectAllButton from './SelectAllButton';
import { useGroupStore } from '../../../../hooks/useGroupStore';
import { GROUP_NAME_KEY } from '../../../../helpers/constants';
import './_style.scss';
import { useSelectAll } from '../../../../hooks/useSelectAll';

const GroupsTableHeader = observer(
  ({ selectedGroups, setSelectedGroups, shouldSelectRecipients, toggleShouldSelectRecipients }) => {
    const { t } = useTranslation();

    const { groups, getGroupsToSelect, isFetching } = useGroupStore();

    const { loadingSelection, checkboxProps } = useSelectAll({
      displayedEntities: groups,
      entitiesFetcher: getGroupsToSelect,
      entityKey: GROUP_NAME_KEY,
      selectedEntities: selectedGroups,
      setSelectedEntities: setSelectedGroups,
      shouldSelectRecipients,
      isFetching
    });

    return (
      <div className="RecipientsSelectionTableHeader">
        <GroupFilters showFullFilter={false} />
        <SelectAllButton
          loadingSelection={loadingSelection}
          selectedEntities={selectedGroups}
          checkboxProps={checkboxProps}
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
  }
);

GroupsTableHeader.propTypes = {
  selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired
};

export default GroupsTableHeader;
