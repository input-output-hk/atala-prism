import { message } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSession } from '../components/providers/SessionContext';
import { UNKNOWN_DID_SUFFIX_ERROR_CODE } from '../helpers/constants';
import Logger from '../helpers/Logger';

export const useCredentialTypes = credentialTypesManager => {
  const { t } = useTranslation();
  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const [credentialTypes, setCredentialTypes] = useState();
  const [isLoading, setIsLoading] = useState(false);

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

  return {
    credentialTypes,
    getCredentialTypes,
    getCredentialTypeDetails,
    isLoading
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
