import React from 'react';
import { observer } from 'mobx-react-lite';
import PropTypes from 'prop-types';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';
import GroupsTableHeader from '../../Molecules/RecipientsTable/GroupsTableHeader';
import ContactsTableHeader from '../../Molecules/RecipientsTable/ContactsTableHeader';

import './_style.scss';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = observer(
  ({ groupsProps, contactsProps, toggleShouldSelectRecipients, shouldSelectRecipients }) => {
    const { t } = useTranslation();

    const renderHelpText = () => (
      <div className="helperTextContainer">
        <span>{t('newCredential.targetsSelection.helpText')}</span>
      </div>
    );

    return (
      <div className="RecipientsSelection">
        <Tabs defaultActiveKey="groups">
          <TabPane key={GROUPS_KEY} tab={t('newCredential.targetsSelection.groups')}>
            {renderHelpText()}
            <GroupsTableHeader
              {...groupsProps}
              shouldSelectRecipients={shouldSelectRecipients}
              toggleShouldSelectRecipients={toggleShouldSelectRecipients}
            />
            <div className="groupsTableContainer">
              <GroupsTable {...groupsProps} shouldSelectRecipients={shouldSelectRecipients} />
            </div>
          </TabPane>
          <TabPane key={SUBJECTS_KEY} tab={t('newCredential.targetsSelection.subjects')}>
            {renderHelpText()}
            <ContactsTableHeader
              {...contactsProps}
              shouldSelectRecipients={shouldSelectRecipients}
              toggleShouldSelectRecipients={toggleShouldSelectRecipients}
            />
            <ConnectionsTable
              {...contactsProps}
              shouldSelectRecipients={shouldSelectRecipients}
              size="xs"
              searchDueGeneralScroll
            />
          </TabPane>
        </Tabs>
      </div>
    );
  }
);

RecipientsSelection.propTypes = {
  groupsProps: PropTypes.shape({
    selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedGroups: PropTypes.func.isRequired
  }).isRequired,
  contactsProps: PropTypes.shape({
    contacts: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedContacts: PropTypes.func.isRequired,
    setContactsFilter: PropTypes.func.isRequired,
    handleContactsRequest: PropTypes.func.isRequired,
    hasMore: PropTypes.bool.isRequired,
    fetchAllContacts: PropTypes.func.isRequired
  }).isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default RecipientsSelection;
