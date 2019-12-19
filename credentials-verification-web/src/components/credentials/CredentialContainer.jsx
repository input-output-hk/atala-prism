import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import { withApi } from '../providers/withApi';

const CredentialContainer = ({ api }) => {
  const { t } = useTranslation();

  // These are the values used to filter credentials
  const [credentialId, setCredentialId] = useState('');
  const [name, setName] = useState('');
  const [credentialType, setCredentialType] = useState('');
  const [category, setCategory] = useState('');
  const [group, setGroup] = useState('');
  const [date, setDate] = useState('');

  // This field is used to know if there are no credentials on
  // the database, independently of the filters
  const [noCredentials, setNoCredentials] = useState(true);

  // These are the arrays from options
  const [credentialTypes, setCredentialTypes] = useState([]);
  const [categories, setCategories] = useState([]);
  const [groups, setGroups] = useState([]);

  // These are the credentials returned from the "backend"
  const [credentials, setCredentials] = useState([]);

  // This is the amount of credentials by the sent query
  const [credentialCount, setCredentialCount] = useState(0);

  // This is used to paginate
  const [offset, setOffset] = useState(0);

  // This is a generic function that calls the getters of the fields that will be used as
  // options for the dropdowns in the filter, sets the results and in case of an exception
  // throws a error depending on the field that failed
  const getTypes = (method, updateField, field) =>
    method()
      .then(response => updateField(response))
      .catch(error => {
        Logger.error(`CredentialContainer: Error while getting ${field}`, error);
        message.error(t('errors.errorGetting', { model: field }));
      });

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
  }, []);
  useEffect(() => {
    getTypes(api.getCategoryTypes, setCategories, 'Categories');
  }, []);
  useEffect(() => {
    getTypes(api.getCredentialsGroups, setGroups, 'Groups');
  }, []);

  useEffect(() => {
    api
      .getTotalCredentials()
      .then(count => setNoCredentials(count === 0))
      .catch(error => {
        Logger.error('CredentialContainer: Error while getting Credentials', error);
        message.error(t('errors.errorGetting', { model: 'Credentials' }));
      });
  }, []);

  useEffect(() => {
    api
      .getCredentials()
      .then(({ credentials: credentialsList, count }) => {
        setCredentials(credentialsList);
        setCredentialCount(count);
      })
      .catch(error => {
        Logger.error('[CredentialContainer.getCredentials] Error while getting Credentials', error);
        message.error(t('errors.errorGetting', { model: 'Credentials' }), 1);
      });
  }, [credentialId, name, credentialType, category, group, offset]);

  const updateFilter = (value, setField) => {
    setOffset(0);
    setField(value);
  };

  const tableProps = {
    credentials,
    credentialCount,
    offset,
    setOffset,
    issueCredential
  };

  const clearFilters = () => {
    setCredentialId('');
    setName('');
    setCredentialType('');
    setCategory('');
    setGroup('');
    setDate();
    setOffset(0);
  };

  const filterProps = {
    credentialId,
    setCredentialId: value => updateFilter(value, setCredentialId),
    name,
    setName: value => updateFilter(value, setName),
    credentialTypes,
    credentialType,
    setCredentialType: value => updateFilter(value, setCredentialType),
    categories,
    category,
    setCategory: value => updateFilter(value, setCategory),
    groups,
    group,
    setGroup: value => updateFilter(value, setGroup),
    date,
    setDate: value => updateFilter(value, setDate),
    clearFilters
  };

  return (
    <Credentials showEmpty={noCredentials} tableProps={tableProps} filterProps={filterProps} />
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
