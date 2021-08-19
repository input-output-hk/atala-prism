import { message } from 'antd';
import _ from 'lodash';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSession } from '../components/providers/SessionContext';
import { SORTING_DIRECTIONS, UNKNOWN_DID_SUFFIX_ERROR_CODE } from '../helpers/constants';
import { filterByInclusion, filterByExactMatch } from '../helpers/filterHelpers';
import Logger from '../helpers/Logger';

const useTemplatesFilters = () => {
  const [name, setName] = useState();
  const [category, setCategory] = useState();
  const [lastEdited, setLastEdited] = useState();

  const values = {
    name,
    category,
    lastEdited
  };

  const setters = {
    setName,
    setCategory,
    setLastEdited
  };

  return {
    values,
    setters
  };
};

export const useCredentialTypes = credentialTypesManager => {
  const { t } = useTranslation();
  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const [credentialTypes, setCredentialTypes] = useState([]);
  const [filteredCredentialTypes, setFilteredCredentialTypes] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const defaultSortBy = 'name';
  const [sortingBy, setSortingBy] = useState(defaultSortBy);
  const [sortDirection, setSortDirection] = useState(SORTING_DIRECTIONS.ascending);

  const filters = useTemplatesFilters();
  const { name, category, lastEdited } = filters.values;

  const applyFrontendFilters = useCallback(
    templatesList =>
      templatesList.filter(item => {
        const matchName = filterByInclusion(name, item.name);
        const matchCategory = filterByExactMatch(category, item.category);
        const matchDate = filterByExactMatch(lastEdited, item.lastEdited);

        return matchName && matchCategory && matchDate;
      }),
    [name, category, lastEdited]
  );

  const applyFrontendSorting = useCallback(
    aCredentialsList =>
      _.orderBy(
        aCredentialsList,
        o => sortingBy && o[sortingBy],
        sortDirection === SORTING_DIRECTIONS.ascending ? 'asc' : 'desc'
      ),
    [sortingBy, sortDirection]
  );

  const getCredentialTypes = useCallback(() => {
    setIsLoading(true);
    return credentialTypesManager
      .getCredentialTypes()
      .then(fetchedCredentialTypes => {
        setCredentialTypes(fetchedCredentialTypes);
        removeUnconfirmedAccountError();
        return fetchedCredentialTypes;
      })
      .catch(error => {
        if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
          showUnconfirmedAccountError();
        } else {
          removeUnconfirmedAccountError();
          Logger.error('[Templates.getTemplates] Error while getting templates', error);
          message.error(t('errors.errorGetting', { model: 'Templates' }));
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [credentialTypesManager, t, removeUnconfirmedAccountError, showUnconfirmedAccountError]);

  const getCredentialTypeDetails = id => credentialTypesManager.getCredentialTypeDetails(id);

  useEffect(() => {
    getCredentialTypes();
  }, [getCredentialTypes]);

  useEffect(() => {
    const sortedCredentials = applyFrontendSorting(credentialTypes);
    const newSortedAndFilteredCredentials = applyFrontendFilters(sortedCredentials);
    setFilteredCredentialTypes(newSortedAndFilteredCredentials);
  }, [
    credentialTypes,
    name,
    category,
    lastEdited,
    sortingBy,
    sortDirection,
    applyFrontendFilters,
    applyFrontendSorting
  ]);

  return {
    credentialTypes,
    filteredCredentialTypes,
    getCredentialTypes,
    getCredentialTypeDetails,
    isLoading,
    filterProps: {
      ...filters.values,
      ...filters.setters
    },
    sortingProps: {
      sortingBy,
      sortDirection,
      setSortingBy,
      setSortDirection
    }
  };
};

export const useTemplateCategories = credentialTypesManager => {
  const { t } = useTranslation();

  const [templateCategories, setTemplateCategories] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const getTemplateCategories = useCallback(() => {
    setIsLoading(true);
    return credentialTypesManager
      .getTemplateCategories()
      .then(fetchedCategories => {
        setTemplateCategories(fetchedCategories);
        return fetchedCategories;
      })
      .catch(error => {
        Logger.error('[Templates.getTemplateCategories] Error while getting categories', error);
        message.error(t('errors.errorGetting', { model: 'Template categories' }));
      })
      .finally(() => setIsLoading(false));
  }, [credentialTypesManager, t]);

  useEffect(() => {
    getTemplateCategories();
  }, [getTemplateCategories]);

  return {
    templateCategories,
    getTemplateCategories,
    isLoading
  };
};
