import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialSummaries from './CredentialSummaries';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { CONNECTION_ACCEPTED } from '../../helpers/constants';

const CredentialSummaryController = ({ api }) => {
  const { t } = useTranslation();

  const [credentialSummaries, setCredentialSummaries] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [noSummaries, setNoSummaries] = useState(false);

  // This functions is the one that interacts with
  // the "backend". Gets the connections and if there
  // are some that match the filter updates the last
  // received id and the property has more for the
  // infinite scroll
  const getCredentialSummaries = (
    oldCredentialSummaries = credentialSummaries,
    _date,
    _name,
    userId
  ) => {
    const { id } = credentialSummaries.length
      ? credentialSummaries[credentialSummaries.length - 1]
      : {};

    setNoSummaries(!id && !_date && !_name);

    return api.subjectsManager
      .getSubjectsAsIssuer(userId, id)
      .then(summariesResponse => {
        const parsedSummaries = summariesResponse
          .filter(({ connectionstatus }) => connectionstatus === CONNECTION_ACCEPTED)
          .map(({ id: credentialId, admissiondate, email, fullname }) => ({
            id: credentialId,
            date: admissiondate,
            user: { email, fullname }
          }));

        if (!parsedSummaries.length) {
          setHasMore(false);
          return;
        }

        setCredentialSummaries(oldCredentialSummaries.concat(parsedSummaries));
      })
      .catch(error => {
        Logger.error('Error while fetching the students', error);
        message.error(t('errors.errorGetting', { model: 'CredentialSummaries' }));
      });
  };

  // This gets the connections when the component mounts
  useEffect(() => {
    getCredentialSummaries();
  }, []);

  // This hook listens for the id and connection cleanup
  // and then gets the connections
  useEffect(() => {
    if (hasMore && !credentialSummaries.length) getCredentialSummaries();
  }, [hasMore, credentialSummaries]);

  const getSubjectCredentials = studentId => api.subjectsManager.getSubjectCredentials(studentId);

  const credentialSummariesProps = {
    getCredentialSummaries,
    credentialSummaries,
    getSubjectCredentials,
    hasMore,
    noSummaries
  };

  return <CredentialSummaries {...credentialSummariesProps} />;
};

CredentialSummaryController.propTypes = {
  api: PropTypes.shape({
    subjectsManager: PropTypes.shape({
      getSubjectCredentials: PropTypes.func,
      getSubjectsAsIssuer: PropTypes.func
    }).isRequired,
    getConnections: PropTypes.func
  }).isRequired
};

export default withApi(CredentialSummaryController);
