import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';
import Groups from './Groups';
import { withApi } from '../providers/witApi';
import { dateAsUnix } from '../../helpers/formatters';

const GroupsContainer = ({ api, selectingProps }) => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState([]);
  const [date, setDate] = useState();
  const [name, setName] = useState('');
  const [hasMore, setHasMore] = useState(true);

  const updateGroups = () => {
    if (!hasMore) return;

    const filterDateAsUnix = dateAsUnix(date);

    const { groupId } = groups.length ? groups[groups.length - 1] : {};

    api
      .getGroups({ name, date: filterDateAsUnix, pageSize: GROUP_PAGE_SIZE, lastId: groupId })
      .then(filteredGroups => {
        if (!filteredGroups.length) {
          setHasMore(false);
          return;
        }
        setGroups(groups.concat(filteredGroups));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.updateGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'), 1);
      });
  };

  useEffect(() => {
    setGroups([]);
    setHasMore(true);
  }, [date, name]);

  useEffect(() => {
    if (!groups.length) {
      updateGroups();
    }
  }, [groups]);

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

  return (
    <Groups
      groups={groups}
      setDate={setDate}
      setName={setName}
      handleGroupDeletion={handleGroupDeletion}
      updateGroups={updateGroups}
      hasMore={hasMore}
      {...selectingProps}
    />
  );
};

GroupsContainer.defaultProps = {
  selectingProps: {}
};

GroupsContainer.propTypes = {
  api: PropTypes.shape().isRequired,
  selectingProps: PropTypes.shape({
    setGroup: PropTypes.func,
    group: PropTypes.func
  })
};

export default withApi(GroupsContainer);
