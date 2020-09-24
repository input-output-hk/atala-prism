import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Col, Tabs, Input, Icon } from 'antd';
import { useTranslation } from 'react-i18next';

import './_style.scss';
import GroupsTable from '../../../groups/Organisms/Tables/GroupsTable';
import ConnectionsTable from '../../../connections/Organisms/table/ConnectionsTable';

const { TabPane } = Tabs;

const GROUPS_KEY = 'groups';
const SUBJECTS_KEY = 'subjects';

const RecipientsSelection = ({
  isIssuer,
  groups,
  selectedGroups,
  setSelectedGroups,
  setGroupsFilter,
  subjects,
  selectedSubjects,
  setSelectedSubjects,
  setSubjectsFilter,
  getSubjects,
  hasMoreSubjects
}) => {
  const { t } = useTranslation();

  // This allows to only render the table that's currently visible
  // to have the infinite scroll work on the correct table
  const [activeKey, setActiveKey] = useState(GROUPS_KEY);

  return (
    <Col type="flex" className="RecipientsSelection">
      <Tabs defaultActiveKey="groups" onChange={setActiveKey}>
        <TabPane key={GROUPS_KEY} tab={t('newCredential.targetsSelection.groups')}>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setGroupsFilter(value)}
          />
          {activeKey === GROUPS_KEY && (
            <GroupsTable
              groups={groups}
              selectedGroups={selectedGroups}
              setSelectedGroups={setSelectedGroups}
            />
          )}
        </TabPane>
        <TabPane key={SUBJECTS_KEY} tab={t('newCredential.targetsSelection.subjects')}>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setSubjectsFilter(value)}
          />
          {activeKey === SUBJECTS_KEY && (
            <ConnectionsTable
              isIssuer={isIssuer}
              subjects={subjects}
              selectedSubjects={selectedSubjects}
              setSelectedSubjects={setSelectedSubjects}
              handleHoldersRequest={getSubjects}
              hasMore={hasMoreSubjects}
            />
          )}
        </TabPane>
      </Tabs>
    </Col>
  );
};

RecipientsSelection.defaultProps = {};

RecipientsSelection.propTypes = {
  isIssuer: PropTypes.func.isRequired,
  groups: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  setGroupsFilter: PropTypes.func.isRequired,
  subjects: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  selectedSubjects: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedSubjects: PropTypes.func.isRequired,
  setSubjectsFilter: PropTypes.func.isRequired,
  getSubjects: PropTypes.func.isRequired,
  hasMoreSubjects: PropTypes.bool.isRequired
};

export default RecipientsSelection;
