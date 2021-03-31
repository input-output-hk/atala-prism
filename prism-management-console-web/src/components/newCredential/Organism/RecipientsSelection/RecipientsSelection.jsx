import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { SearchOutlined, WarningOutlined } from '@ant-design/icons';
import { Tabs, Input, Checkbox } from 'antd';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';
import { withApi } from '../../../providers/withApi';
import {
  getCheckedAndIndeterminateProps,
  handleSelectAll
} from '../../../../helpers/selectionHelpers';
import { CONTACT_ID_KEY } from '../../../../helpers/constants';

import './_style.scss';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = ({
  groupsProps,
  contactsProps,
  toggleShouldSelectRecipients,
  shouldSelectRecipients
}) => {
  const { groups, selectedGroups, setSelectedGroups, setGroupsFilter } = groupsProps;
  const {
    contacts,
    setSelectedContacts,
    selectedContacts,
    setContactsFilter,
    hasMore,
    fetchAll
  } = contactsProps;

  const { t } = useTranslation();
  const [loadingSelection, setLoadingSelection] = useState(false);

  const handleSelectAllContacts = ev =>
    handleSelectAll({
      ev,
      setSelected: setSelectedContacts,
      entities: contacts,
      hasMore,
      idKey: CONTACT_ID_KEY,
      fetchAll,
      setLoading: setLoadingSelection
    });

  const handleSelectAllGroups = ev => {
    const { checked } = ev.target;
    setSelectedGroups(checked ? groups.map(g => g.name) : []);
  };

  // This allows to only render the table that's currently visible
  // to have the infinite scroll work on the correct table
  const [activeKey, setActiveKey] = useState(GROUPS_KEY);

  const selectAllGroupsProps = {
    ...getCheckedAndIndeterminateProps(groups, selectedGroups),
    disabled: !shouldSelectRecipients,
    onChange: handleSelectAllGroups
  };

  const selectAllContactsProps = {
    ...getCheckedAndIndeterminateProps(contacts, selectedContacts),
    disabled: !shouldSelectRecipients,
    onChange: handleSelectAllContacts
  };

  return (
    <div className="RecipientsSelection">
      <Tabs defaultActiveKey="groups" onChange={setActiveKey}>
        <TabPane key={GROUPS_KEY} tab={t('newCredential.targetsSelection.groups')}>
          <div className="selectGroupSubtitle">
            <span>{t('newCredential.targetsSelection.selectGroup')}</span>
          </div>
          <div className="selectionContainer">
            <Input
              className="selectionGroups"
              placeholder={t('groups.filters.search')}
              prefix={<SearchOutlined />}
              onChange={({ target: { value } }) => setGroupsFilter(value)}
            />
            <div className="selectGroupsCheckbox">
              <Checkbox className="groupsCheckbox" {...selectAllGroupsProps}>
                <span>
                  {t('newCredential.targetsSelection.selectAll')}
                  {selectedGroups.length ? `  (${selectedGroups.length})  ` : null}
                </span>
              </Checkbox>
            </div>
            <div className="selectGroupCheckbox noRecipientsCheckbox">
              <Checkbox
                className="checkboxReverse"
                onChange={toggleShouldSelectRecipients}
                checked={!shouldSelectRecipients}
              >
                <WarningOutlined className="icon" /> {t('newCredential.targetsSelection.checkbox')}
              </Checkbox>
            </div>
          </div>

          {activeKey === GROUPS_KEY && (
            <div className="groupsTableContainer">
              <GroupsTable
                {...groupsProps}
                shouldSelectRecipients={shouldSelectRecipients}
                size="xs"
              />
            </div>
          )}
        </TabPane>
        <TabPane key={SUBJECTS_KEY} tab={t('newCredential.targetsSelection.subjects')}>
          <div className="selectGroupSubtitle">
            <span>{t('newCredential.targetsSelection.selectGroup')}</span>
          </div>
          <div className="selectionContainer">
            <Input
              className="selectionGroups"
              setSelectedSubjects
              placeholder={t('groups.filters.search')}
              prefix={<SearchOutlined />}
              onChange={({ target: { value } }) => setContactsFilter(value)}
            />
            <div className="selectContactsCheckbox">
              <Checkbox className="contactsCheckbox" {...selectAllContactsProps}>
                {loadingSelection ? (
                  <div className="loadingSelection">
                    <PulseLoader size={3} color="#FFAEB3" />
                  </div>
                ) : (
                  <span>
                    {t('newCredential.targetsSelection.selectAll')}
                    {selectedContacts.length ? `  (${selectedContacts.length})  ` : null}
                  </span>
                )}
              </Checkbox>
            </div>
            <div className="selectContactsCheckbox noRecipientsCheckbox">
              <Checkbox
                className="checkboxReverse"
                onChange={toggleShouldSelectRecipients}
                checked={!shouldSelectRecipients}
              >
                <WarningOutlined className="icon" />
                {t('newCredential.targetsSelection.checkbox')}
              </Checkbox>
            </div>
          </div>
          {activeKey === SUBJECTS_KEY && (
            <ConnectionsTable
              {...contactsProps}
              shouldSelectRecipients={shouldSelectRecipients}
              size="xs"
            />
          )}
        </TabPane>
      </Tabs>
    </div>
  );
};

RecipientsSelection.propTypes = {
  groupsProps: PropTypes.shape({
    groups: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedGroups: PropTypes.func.isRequired,
    setGroupsFilter: PropTypes.func.isRequired
  }).isRequired,
  contactsProps: PropTypes.shape({
    contacts: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedContacts: PropTypes.func.isRequired,
    setContactsFilter: PropTypes.func.isRequired,
    handleContactsRequest: PropTypes.func.isRequired,
    hasMore: PropTypes.bool.isRequired,
    fetchAll: PropTypes.func.isRequired
  }).isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default withApi(RecipientsSelection);
