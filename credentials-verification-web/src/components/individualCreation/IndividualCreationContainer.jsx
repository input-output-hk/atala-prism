import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import IndividualCreation from './IndividualCreation';
import { withApi } from '../providers/withApi';
import { withRedirector } from '../providers/withRedirector';
import Logger from '../../helpers/Logger';

const createBlankIndividual = key => ({ key, fullName: '', email: '' });
const defaultIndividualList = [createBlankIndividual(0)];

const IndividualCreationContainer = ({ api, redirector: { redirectToConnections } }) => {
  const { t } = useTranslation();
  console.log('default indiv');
  console.log('default indiv', defaultIndividualList);
  const [individuals, setIndividuals] = useState(defaultIndividualList);
  const [invalidIndividuals, setIndvalidIndividuals] = useState(false);

  const addNewIndividual = () => {
    const { key = 0 } = _.last(individuals) || {};

    const newData = createBlankIndividual(key + 1);
    const newDataSource = individuals.concat(newData);

    setIndividuals(newDataSource);
  };

  const editIndividual = individual => {
    const clonedIndividuals = [...individuals];

    const index = clonedIndividuals.findIndex(({ key }) => individual.key === key);
    const item = clonedIndividuals[index];
    const editedIndividual = Object.assign({}, item, individual);

    clonedIndividuals.splice(index, 1, editedIndividual);

    setIndividuals(clonedIndividuals);
  };

  const deleteIndividual = key => {
    const filteredIndividuals = individuals.filter(
      ({ key: individualKey }) => key !== individualKey
    );

    setIndividuals(filteredIndividuals);
  };

  const saveIndividuals = () => {
    if (!individuals.length) return redirectToConnections();

    const invalidIndividual = individuals.reduce((accumulator, { fullName, email }) => {
      const isInvalid = !fullName || !email;

      return accumulator || isInvalid;
    }, false);

    setIndvalidIndividuals(invalidIndividual);

    if (invalidIndividual) return;

    const creationPromises = individuals.map(({ fullName, email }) =>
      api.credentialStore.createHolder(fullName, email)
    );

    Promise.all(creationPromises)
      .then(() => {
        message.success(t('individualCreation.success'));
        redirectToConnections();
      })
      .catch(error => {
        Logger.error('Error while creating individual', error);
        message.error('Error while saving the individual');
      });
  };

  const tableProps = {
    individuals,
    deleteIndividual,
    editIndividual
  };

  return (
    <IndividualCreation
      saveIndividual={addNewIndividual}
      saveIndividuals={saveIndividuals}
      tableProps={tableProps}
      invalidIndividuals={invalidIndividuals}
    />
  );
};

IndividualCreationContainer.propTypes = {
  api: PropTypes.shape({
    credentialStore: PropTypes.shape({ createHolder: PropTypes.func }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToConnections: PropTypes.func }).isRequired
};

export default withApi(withRedirector(IndividualCreationContainer));
