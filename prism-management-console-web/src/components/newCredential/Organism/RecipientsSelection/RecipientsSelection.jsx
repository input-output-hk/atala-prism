import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { SearchOutlined, WarningOutlined } from '@ant-design/icons';
import { Col, Tabs, Input, Checkbox } from 'antd';
import { useTranslation } from 'react-i18next';

import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';
import { withApi } from '../../../providers/withApi';
import { useSelectAllContacts, useSelectAllGroups } from '../../../../hooks/useSelectAll';

import './_style.scss';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = ({
  api,
  groups,
  selectedGroups,
  setSelectedGroups,
  setGroupsFilter,
  subjects,
  selectedSubjects,
  setSelectedSubjects,
  setSubjectsFilter,
  getSubjects,
  toggleShouldSelectRecipients,
  shouldSelectRecipients
}) => {
  const { t } = useTranslation();
  const handleSelectAllContacts = useSelectAllContacts(api.contactsManager, setSelectedSubjects);
  const handleSelectAllGroups = useSelectAllGroups(api.groupsManager, setSelectedGroups);

  // This allows to only render the table that's currently visible
  // to have the infinite scroll work on the correct table
  const [activeKey, setActiveKey] = useState(GROUPS_KEY);

  const selectAllGroupsProps = {
    checked: selectedGroups.length === groups.length,
    indeterminate: selectedGroups.length && selectedGroups.length !== groups.length,
    disabled: !shouldSelectRecipients,
    onChange: handleSelectAllGroups
  };

  const selectAllSubjectsProps = {
    checked: selectedSubjects.length === subjects.length,
    indeterminate: selectedSubjects.length && selectedSubjects.length !== subjects.length,
    disabled: !shouldSelectRecipients,
    onChange: handleSelectAllContacts
  };

  return (
    <Col type="flex" className="RecipientsSelection">
      <Tabs defaultActiveKey="groups" onChange={setActiveKey}>
        <TabPane key={GROUPS_KEY} tab={t('newCredential.targetsSelection.groups')}>
          <div className="selectGroupSubtitle">
            <span>{t('newCredential.targetsSelection.selectGroup')}</span>
          </div>
          <div className="selectionGroupsContainer">
            <Input
              className="selectionGroups"
              placeholder={t('groups.filters.search')}
              prefix={<SearchOutlined />}
              onChange={({ target: { value } }) => setGroupsFilter(value)}
            />
            <div className="selectGroupCheckbox">
              <Checkbox className="checkboxReverse" {...selectAllGroupsProps}>
                {`${t('newCredential.targetsSelection.selectAll')} (${selectedGroups.length})`}
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
                groups={groups}
                selectedGroups={selectedGroups}
                setSelectedGroups={setSelectedGroups}
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
          <div className="selectionGroupsContainer">
            <Input
              className="selectionGroups"
              placeholder={t('groups.filters.search')}
              prefix={<SearchOutlined />}
              onChange={({ target: { value } }) => setSubjectsFilter(value)}
            />
            <div className="selectGroupCheckbox">
              <Checkbox className="checkboxReverse" {...selectAllSubjectsProps}>
                {`${t('newCredential.targetsSelection.selectAll')} (${selectedSubjects.length})`}
              </Checkbox>
            </div>
            <div className="selectGroupCheckbox noRecipientsCheckbox">
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
              contacts={subjects}
              selectedContacts={selectedSubjects}
              setSelectedContacts={setSelectedSubjects}
              handleContactsRequest={getSubjects}
              shouldSelectRecipients={shouldSelectRecipients}
              size="xs"
            />
          )}
        </TabPane>
      </Tabs>
    </Col>
  );
};

RecipientsSelection.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func
    }).isRequired,
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    }).isRequired
  }).isRequired,
  groups: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  setGroupsFilter: PropTypes.func.isRequired,
  subjects: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  selectedSubjects: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedSubjects: PropTypes.func.isRequired,
  setSubjectsFilter: PropTypes.func.isRequired,
  getSubjects: PropTypes.func.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default withApi(RecipientsSelection);
