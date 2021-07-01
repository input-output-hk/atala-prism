import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { useSession } from '../components/providers/SessionContext';
import {
  GROUP_PAGE_SIZE,
  SORTING_DIRECTIONS,
  GROUP_SORTING_KEYS,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../helpers/constants';
import { useDebounce } from './useDebounce';
import Logger from '../helpers/Logger';

export const useGroups = groupsManager => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);

  const [name, setName] = useState('');
  const [dateRange, setDateRange] = useState([]);
  const [sortingKey, setSortingKey] = useState(GROUP_SORTING_KEYS.name);
  const [sortingDirection, setSortingDirection] = useState(SORTING_DIRECTIONS.ascending);

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const getGroups = useDebounce(
    ({
      currentGroups = [],
      pageSize = GROUP_PAGE_SIZE,
      direction = SORTING_DIRECTIONS.ascending,
      field = GROUP_SORTING_KEYS.name,
      nameFilter = '',
      dateFilter = []
    }) => {
      setSearching(true);

      const [createdAfter, createdBefore] = dateFilter;

      return groupsManager
        .getGroups({
          offset: currentGroups.length,
          pageSize,
          sort: { field, direction },
          filter: {
            name: nameFilter,
            createdBefore,
            createdAfter
          }
        })
        .then(({ groupsList: newGroups }) => {
          if (newGroups.length < GROUP_PAGE_SIZE) setHasMore(false);
          setGroups(currentGroups.concat(newGroups));
          removeUnconfirmedAccountError();
        })
        .catch(error => {
          Logger.error('[GroupsContainer.getGroups] Error: ', error);
          if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
            setHasMore(false);
            showUnconfirmedAccountError();
          } else {
            removeUnconfirmedAccountError();
            message.error(t('errors.errorGetting', { model: 'groups' }));
          }
        })
        .finally(() => {
          setLoading(false);
          setSearching(false);
        });
    },
    [groupsManager, removeUnconfirmedAccountError, showUnconfirmedAccountError, t]
  );

  const loadGroups = useRef(() => getGroups({}));

  useEffect(() => {
    loadGroups.current();
  }, []);

  useEffect(() => {
    if (!loading)
      getGroups({
        currentGroups: [],
        direction: sortingDirection,
        field: sortingKey,
        nameFilter: name,
        dateFilter: dateRange
      });
  }, [loading, sortingDirection, sortingKey, name, dateRange, getGroups]);

  const getMoreGroups = useCallback(() => {
    getGroups({
      currentGroups: groups,
      direction: sortingDirection,
      field: sortingKey,
      nameFilter: name,
      dateFilter: dateRange
    });
  }, [dateRange, getGroups, groups, name, sortingDirection, sortingKey]);

  return {
    groups,
    loading,
    searching,
    hasMore,
    name,
    setName,
    dateRange,
    setDateRange,
    sortingKey,
    setSortingKey,
    sortingDirection,
    setSortingDirection,
    getGroups,
    getMoreGroups
  };
};
