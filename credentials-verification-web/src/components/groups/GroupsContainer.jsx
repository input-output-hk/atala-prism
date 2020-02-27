import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';
import Groups from './Groups';
import { withApi } from '../providers/withApi';
import { dateAsUnix } from '../../helpers/formatters';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';

const GroupsContainer = ({ api, selectingProps }) => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState([]);
  const [hasMore, setHasMore] = useState(true);

  const updateGroups = (oldGroups = groups, date, name) => {
    if (!hasMore) return;

    const filterDateAsUnix = dateAsUnix(date);

    const { groupId } = getLastArrayElementOrEmpty(oldGroups);

    return api
      .getGroups({ name, date: filterDateAsUnix, pageSize: GROUP_PAGE_SIZE, lastId: groupId })
      .then(filteredGroups => {
        if (!filteredGroups.length) {
          setHasMore(false);
          return;
        }

        setGroups(oldGroups.concat(filteredGroups));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.updateGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'));
      });
  };

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
        message.success(t('groups.deletionSuccess', { groupName }));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.handleGroupDeletion] Error: ', error);
        message.error(t('errors.errorDeletingGroup', { groupName }));
      });

  return (
    <Groups
      groups={groups}
      updateGroups={(oldGroups, date, name) => {
        setHasMore(true);
        updateGroups(oldGroups, date, name);
      }}
      handleGroupDeletion={handleGroupDeletion}
      hasMore={hasMore}
      {...selectingProps}
    />
  );
};

GroupsContainer.defaultProps = {
  selectingProps: {},
  fullInfo: true
};

GroupsContainer.propTypes = {
  api: PropTypes.shape().isRequired,
  selectingProps: PropTypes.shape({
    setGroup: PropTypes.func,
    group: PropTypes.func
  }),
  fullInfo: PropTypes.bool
};

export default withApi(GroupsContainer);
