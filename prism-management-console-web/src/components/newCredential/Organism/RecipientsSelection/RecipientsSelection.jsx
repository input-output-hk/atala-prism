import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import PropTypes from 'prop-types';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';
import GroupsTableHeader from '../../Molecules/RecipientsTable/GroupsTableHeader';
import ContactsTableHeader from '../../Molecules/RecipientsTable/ContactsTableHeader';

import './_style.scss';
import { useContactStore, useContactUiState } from '../../../../hooks/useContactStore';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = observer(
  ({
    selectedGroups,
    setSelectedGroups,
    selectedContacts,
    setSelectedContacts,
    toggleShouldSelectRecipients,
    shouldSelectRecipients
  }) => {
    const { t } = useTranslation();

    const { hasFiltersApplied, isSearching, isSorting } = useContactUiState();
    const {
      contacts,
      initContactStore,
      isLoadingFirstPage,
      fetchMoreData,
      isFetching,
      hasMore
    } = useContactStore();

    useEffect(() => {
      initContactStore();
    }, [initContactStore]);

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
              selectedGroups={selectedGroups}
              setSelectedGroups={setSelectedGroups}
              shouldSelectRecipients={shouldSelectRecipients}
              toggleShouldSelectRecipients={toggleShouldSelectRecipients}
            />
            <div className="groupsTableContainer">
              <GroupsTable
                selectedGroups={selectedGroups}
                setSelectedGroups={setSelectedGroups}
                shouldSelectRecipients={shouldSelectRecipients}
              />
            </div>
          </TabPane>
          <TabPane key={SUBJECTS_KEY} tab={t('newCredential.targetsSelection.subjects')}>
            {renderHelpText()}
            <ContactsTableHeader
              setSelectedContacts={setSelectedContacts}
              selectedContacts={selectedContacts}
              shouldSelectRecipients={shouldSelectRecipients}
              toggleShouldSelectRecipients={toggleShouldSelectRecipients}
            />
            <ConnectionsTable
              contacts={contacts}
              fetchMoreData={fetchMoreData}
              hasMore={hasMore}
              hasFiltersApplied={hasFiltersApplied}
              isLoading={isLoadingFirstPage || isLoadingFirstPage || isSorting}
              isFetchingMore={isFetching || isSearching}
              setSelectedContacts={setSelectedContacts}
              selectedContacts={selectedContacts}
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
    selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedContacts: PropTypes.func.isRequired
  }).isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default RecipientsSelection;
