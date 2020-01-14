import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import { withApi } from '../providers/withApi';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import { CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';

const CredentialContainer = ({ api }) => {
  const { t } = useTranslation();

  // These are the values used to filter credentials

  // This field is used to know if there are no credentials on
  // the database, independently of the filters
  const [noCredentials, setNoCredentials] = useState(true);

  // These are the arrays from options
  const [credentialTypes, setCredentialTypes] = useState([]);
  const [categories, setCategories] = useState([]);
  const [groups, setGroups] = useState([]);

  // These are the credentials returned from the "backend"
  const [credentials, setCredentials] = useState([]);
  const [hasMore, setHasMore] = useState(false);

  // This is a generic function that calls the getters of the fields that will be used as
  // options for the dropdowns in the filter, sets the results and in case of an exception
  // throws a error depending on the field that failed
  const getTypes = (_method, updateField, field) => updateField([`Mock ${field}`]);
  // method()
  //   .then(response => updateField(response))
  //   .catch(error => {
  //     Logger.error(`CredentialContainer: Error while getting ${field}`, error);
  //     message.error(t('errors.errorGetting', { model: field }));
  //   });

  const issueCredential = credential => {
    api
      .issueCredential(credential)
      .then(() => message.success(t('credentials.successfullyIssued')))
      .catch(error => {
        Logger.error('CredentialContainer: Error while sending credential', error);
        message.error(t('errors.issueCredential'));
      });
  };

  useEffect(() => {
    getTypes(api.getCredentialTypes, setCredentialTypes, 'Credentials');
    getTypes(api.getCategoryTypes, setCategories, 'Categories');
    getTypes(api.getCredentialsGroups, setGroups, 'Groups');
  }, []);

  const fetchCredentials = (
    isFirstCall,
    oldCredentials = credentials,
    _credentialId,
    _name,
    _credentialType,
    _category,
    _group,
    _date
  ) => {
    const { id } = getLastArrayElementOrEmpty(oldCredentials);

    return api
      .getCredentials(CREDENTIAL_PAGE_SIZE, id)
      .then(({ credentials: credentialsList }) => {
        if (isFirstCall) setNoCredentials(!credentialsList.length);

        if (!credentialsList.length) {
          setHasMore(false);
          return;
        }

        setCredentials(oldCredentials.concat(credentialsList));
      })
      .catch(error => {
        Logger.error('[CredentialContainer.getCredentials] Error while getting Credentials', error);
        message.error(t('errors.errorGetting', { model: 'Credentials' }), 1);
      });
  };

  useEffect(() => {
    fetchCredentials(true);
    setHasMore(true);
  }, []);

  useEffect(() => {
    if (!credentials.length && hasMore) fetchCredentials();
  }, [credentials, hasMore]);

  const tableProps = {
    credentials,
    hasMore,
    issueCredential
  };

  const filterProps = {
    credentialTypes,
    categories,
    groups
  };

  return (
    <Credentials
      showEmpty={noCredentials}
      tableProps={tableProps}
      fetchCredentials={fetchCredentials}
      filterProps={filterProps}
    />
  );
};

CredentialContainer.propTypes = {
  api: PropTypes.shape({
    getCredentialTypes: PropTypes.func.isRequired,
    getCategoryTypes: PropTypes.func.isRequired,
    getCredentialsGroups: PropTypes.func.isRequired,
    getTotalCredentials: PropTypes.func.isRequired,
    issueCredential: PropTypes.func.isRequired,
    getCredentials: PropTypes.func.isRequired,
    deleteCredential: PropTypes.func.isRequired
  }).isRequired
};

export default withApi(CredentialContainer);
