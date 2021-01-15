import React, { useEffect, useState } from 'react';
import _ from 'lodash';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import { GROUP_PAGE_SIZE, UNKNOWN_DID_SUFFIX_ERROR_CODE } from '../../helpers/constants';
import Groups from './Groups';
import { withApi } from '../providers/withApi';
import { dateAsUnix } from '../../helpers/formatters';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import { useSession } from '../providers/SessionContext';

const GroupsContainer = ({ api }) => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const updateGroups = (oldGroups = groups, date, name) => {
    if (!hasMore) return;

    const filterDateAsUnix = dateAsUnix(date);

    const { groupId } = getLastArrayElementOrEmpty(oldGroups);

    return api.groupsManager
      .getGroups({ name, date: filterDateAsUnix, pageSize: GROUP_PAGE_SIZE, lastId: groupId })
      .then(filteredGroups => {
        if (!filteredGroups.length) {
          setHasMore(false);
          return;
        }
        const newGroups = _.uniqBy(oldGroups.concat(filteredGroups), 'id');
        setGroups(newGroups);
        removeUnconfirmedAccountError();
      })
      .catch(error => {
        Logger.error('[GroupsContainer.updateGroups] Error: ', error);
        if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
          showUnconfirmedAccountError();
        } else {
          removeUnconfirmedAccountError();
          message.error(t('errors.errorGetting', { model: 'groups' }));
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!groups.length) {
      setLoading(true);
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

  const copyGroup = ({ numberofcontacts, name }, copyName) =>
    api.groupsManager
      .createGroup(copyName)
      .then(({ id }) =>
        api.contactsManager
          .getContacts(null, numberofcontacts, name)
          .then(contacts =>
            api.groupsManager.updateGroup(id, contacts.map(({ contactid }) => contactid))
          )
      )
      .then(() => {
        updateGroups();
        message.success(t('groups.copy.success'));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.copyGroup] Error: ', error);
        message.error(t('errors.errorCopyingGroup'));
      });

  return (
    <Groups
      groups={groups}
      updateGroups={(oldGroups, date, name) => {
        setHasMore(true);
        updateGroups(oldGroups, date, name);
      }}
      copyGroup={copyGroup}
      handleGroupDeletion={handleGroupDeletion}
      loading={loading}
      hasMore={hasMore}
    />
  );
};

GroupsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(GroupsContainer);
