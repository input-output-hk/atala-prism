import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import NewCredential from './NewCredential';
import { withApi } from '../providers/withApi';
import TypeSelection from './Organism/TypeSelection/TypeSelection';
import RecipientsSelection from './Organism/RecipientsSelection/RecipientsSelection';
import { withRedirector } from '../providers/withRedirector';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import Logger from '../../helpers/Logger';

const NewCredentialContainer = ({ api }) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(0);

  const [credentialType, setCredentialType] = useState();

  const [groups, setGroups] = useState([]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groupsFilter, setGroupsFilter] = useState('');
  const [filteredGroups, setFilteredGroups] = useState([]);

  const [subjects, setSubjects] = useState([]);
  const [selectedSubjects, setSelectedSubjects] = useState([]);
  const [subjectsFilter, setSubjectsFilter] = useState('');
  const [filteredSubjects, setFilteredSubjects] = useState([]);
  const [hasMoreSubjects, setHasMoreSubjects] = useState(true);

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  const getGroups = () =>
    api.groupsManager
      .getGroups()
      .then(allGroups => setGroups(allGroups))
      .catch(error => {
        Logger.error('[NewCredentailContainer.getGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'));
      });

  const getSubjects = () => {
    const getIndividuals = api.getIndividuals(api.wallet.isIssuer());
    const { id: lastId } = getLastArrayElementOrEmpty(subjects);

    return getIndividuals(HOLDER_PAGE_SIZE, lastId)
      .then(connections => {
        if (connections.length < HOLDER_PAGE_SIZE) setHasMoreSubjects(false);

        const subjectsWithKey = connections.map(
          ({ status: holderStatus, connectionstatus, id: holderId, individualid, ...rest }) => {
            const id = holderId || individualid;
            const status = holderStatus !== undefined ? holderStatus : connectionstatus;

            return Object.assign({}, rest, { key: id, status, id });
          }
        );

        const updatedSubjects = subjects.concat(subjectsWithKey);
        setSubjects(updatedSubjects);
      })
      .catch(error => {
        Logger.error('[NewCredentailContainer.getSubjects] Error while getting connections', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }));
      });
  };

  useEffect(() => {
    if (!groups.length) getGroups();
    if (!subjects.length) getSubjects();
  });

  const filterGroups = filter => setFilteredGroups(filterBy(groups, filter, 'name'));
  const filterSubjects = filter => setFilteredSubjects(filterBy(subjects, filter, 'fullname'));

  const filterBy = (toFilter, filter, key) =>
    toFilter.filter(({ [key]: name }) => name.toLowerCase().includes(filter.toLowerCase()));

  useEffect(() => {
    filterGroups(groupsFilter);
  }, [groupsFilter, groups]);

  useEffect(() => {
    filterSubjects(subjectsFilter);
  }, [subjectsFilter, subjects]);

  useEffect(() => {
    if (hasMoreSubjects && filteredSubjects.length < HOLDER_PAGE_SIZE) getSubjects();
  }, [filteredSubjects]);

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return (
          <TypeSelection
            credentialTypes={credentialTypes}
            onTypeSelection={setCredentialType}
            selectedType={credentialType}
          />
        );
      case 1:
        return (
          <RecipientsSelection
            isIssuer={() => api.wallet.isIssuer()}
            groups={filteredGroups}
            selectedGroups={selectedGroups}
            setSelectedGroups={setSelectedGroups}
            setGroupsFilter={setGroupsFilter}
            subjects={filteredSubjects}
            setSelectedSubjects={setSelectedSubjects}
            selectedSubjects={selectedSubjects}
            setSubjectsFilter={setSubjectsFilter}
            getSubjects={getSubjects}
            hasMoreSubjects={hasMoreSubjects}
          />
        );
      case 2:
        return null; // TODO: Implement credential information import
      default:
        return null; // TODO: Implement credential visualisation + signing
    }
  };

  const hasSelectedRecipients = selectedGroups.length || selectedSubjects.length;

  return (
    <NewCredential
      currentStep={currentStep}
      changeStep={setCurrentStep}
      renderStep={renderStep}
      credentialType={credentialType}
      hasSelectedRecipients={hasSelectedRecipients}
    />
  );
};

NewCredentialContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func,
      createCredential: PropTypes.func
    }).isRequired,
    getIndividuals: PropTypes.func,
    wallet: PropTypes.shape({ isIssuer: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
