import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import SelectAllButton from './SelectAllButton';
import { CONTACT_ID_KEY } from '../../../../helpers/constants';
import { useContactStore, useContactUiState } from '../../../../hooks/useContactStore';
import ConnectionsFilter from '../../../connections/Molecules/filter/ConnectionsFilter';
import { useSelectAll } from '../../../../hooks/useSelectAll';
import './_style.scss';

const ContactsTableHeader = observer(
  ({
    selectedContacts,
    setSelectedContacts,
    shouldSelectRecipients,
    toggleShouldSelectRecipients
  }) => {
    const { t } = useTranslation();

    const { getContactsToSelect, isFetching } = useContactStore();
    const { displayedContacts } = useContactUiState();

    const { loadingSelection, checkboxProps } = useSelectAll({
      displayedEntities: displayedContacts,
      entitiesFetcher: getContactsToSelect,
      entityKey: CONTACT_ID_KEY,
      selectedEntities: selectedContacts,
      setSelectedEntities: setSelectedContacts,
      shouldSelectRecipients,
      isFetching
    });

    return (
      <div className="RecipientsSelectionTableHeader">
        <ConnectionsFilter fullFilters={false} />
        <SelectAllButton
          loadingSelection={loadingSelection}
          selectedEntities={selectedContacts}
          checkboxProps={checkboxProps}
        />
        <div className="RecipientSelectionCheckbox NoRecipientsCheckbox">
          <Checkbox
            className="CheckboxReverse"
            onChange={toggleShouldSelectRecipients}
            checked={!shouldSelectRecipients}
          >
            <WarningOutlined className="icon" />
            {t('newCredential.targetsSelection.checkbox')}
          </Checkbox>
        </div>
      </div>
    );
  }
);

ContactsTableHeader.propTypes = {
  selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedContacts: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired
};

export default ContactsTableHeader;
