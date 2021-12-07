import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import PropTypes from 'prop-types';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';
import GroupsTableHeader from '../../Molecules/RecipientsTable/GroupsTableHeader';
import ContactsTableHeader from '../../Molecules/RecipientsTable/ContactsTableHeader';
import { useCreateCredentialPageStore } from '../../../../hooks/useCreateCredentialPageStore';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = observer(({ toggleShouldSelectRecipients, shouldSelectRecipients }) => {
  const { t } = useTranslation();

  const {
    initRecipients,
    isInitRecipientsInProgress,
    contacts,
    hasMoreContacts,
    hasContactsFiltersApplied,
    isSearchingContacts,
    isFetchingMoreContacts,
    fetchMoreContacts,
    // contacts select all
    selectedContactIds,
    handleContactsCherryPickSelection,
    // groups
    groups,
    hasMoreGroups,
    hasGroupsFiltersApplied,
    isSearchingGroups,
    isFetchingMoreGroups,
    fetchMoreGroups,
    // groups select all
    selectedGroups,
    handleGroupsCherryPickSelection
  } = useCreateCredentialPageStore();

  useEffect(() => {
    initRecipients();
  }, [initRecipients]);

  const renderHelpText = () => (
    <div className="helperTextContainer">
      <span>{t('newCredential.targetsSelection.helpText')}</span>
    </div>
  );

  if (isInitRecipientsInProgress) {
    return <SimpleLoading size="md" />;
  }

  return (
    <div className="RecipientsSelection">
      <Tabs defaultActiveKey="groups">
        <TabPane key={GROUPS_KEY} tab={t('newCredential.targetsSelection.groups')}>
          {renderHelpText()}
          <GroupsTableHeader
            shouldSelectRecipients={shouldSelectRecipients}
            toggleShouldSelectRecipients={toggleShouldSelectRecipients}
          />
          <div className="groupsTableContainer">
            <GroupsTable
              groups={groups}
              fetchMoreData={fetchMoreGroups}
              isFetchingMore={isFetchingMoreGroups}
              hasMore={hasMoreGroups}
              hasFiltersApplied={hasGroupsFiltersApplied}
              isLoading={isSearchingGroups}
              selectedGroups={selectedGroups}
              onSelect={handleGroupsCherryPickSelection}
              shouldSelectRecipients={shouldSelectRecipients}
            />
          </div>
        </TabPane>
        <TabPane key={SUBJECTS_KEY} tab={t('newCredential.targetsSelection.subjects')}>
          {renderHelpText()}
          <ContactsTableHeader
            shouldSelectRecipients={shouldSelectRecipients}
            toggleShouldSelectRecipients={toggleShouldSelectRecipients}
          />
          <ConnectionsTable
            contacts={contacts}
            fetchMoreData={fetchMoreContacts}
            hasMore={hasMoreContacts}
            hasFiltersApplied={hasContactsFiltersApplied}
            isLoading={isSearchingContacts}
            isFetchingMore={isFetchingMoreContacts}
            onSelect={handleContactsCherryPickSelection}
            selectedContactIds={selectedContactIds}
            shouldSelectRecipients={shouldSelectRecipients}
            size="xs"
          />
        </TabPane>
      </Tabs>
    </div>
  );
});

RecipientsSelection.propTypes = {
  toggleShouldSelectRecipients: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default RecipientsSelection;
