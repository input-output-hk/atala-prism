import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import moment from 'moment';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';
import Groups from './Groups';
import { withApi } from '../providers/witApi';
import { dateAsUnix } from '../../helpers/formatters';

const GroupsContainer = ({ api }) => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState([]);
  const [groupCount, setGroupCount] = useState(0);
  const [date, setDate] = useState();
  const [name, setName] = useState('');
  const [offset, setOffset] = useState(0);

  const updateGroups = () => {
    const filterDateAsUnix = dateAsUnix(date);

    api
      .getGroups({ name, date: filterDateAsUnix, offset, pageSize: GROUP_PAGE_SIZE })
      .then(({ groups: filteredGroups, groupsCount: count }) => {
        setGroups(filteredGroups);
        setGroupCount(count);
      })
      .catch(error => {
        Logger.error('[GroupsContainer.updateGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'), 1);
      });
  };

  useEffect(() => {
    updateGroups();
  }, [date, name, offset]);

  const handleGroupDeletion = (id, groupName) =>
    api
      .deleteGroup({ id })
      .then(() => {
        updateGroups();
        message.success(t('groups.deletionSuccess', { groupName }), 1);
      })
      .catch(error => {
        Logger.error('[GroupsContainer.handleGroupDeletion] Error: ', error);
        message.error(t('errors.errorDeletingGroup', { groupName }), 1);
      });

  const updateFilter = (value, setField) => {
    setOffset(0);
    setField(value);
  };

  return (
    <Groups
      fullInfo
      groups={groups}
      count={groupCount}
      offset={offset}
      setOffset={setOffset}
      setDate={value => updateFilter(value, setDate)}
      setName={value => updateFilter(value, setName)}
      handleGroupDeletion={handleGroupDeletion}
    />
  );
};

GroupsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(GroupsContainer);
