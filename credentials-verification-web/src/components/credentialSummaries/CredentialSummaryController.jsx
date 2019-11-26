import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialSummaries from './CredentialSummaries';
import { withApi } from '../providers/witApi';
import Logger from '../../helpers/Logger';

const CredentialSummaryController = ({ api: { getConnectionsPaginated } }) => {
  const { t } = useTranslation();

  const [credentialSummaries, setCredentialSummaries] = useState([]);
  const [date, setDate] = useState();
  const [name, setName] = useState('');
  const [hasMore, setHasMore] = useState(true);
  const [lastId, setLastId] = useState(0);

  const handleCredentialSummaryDeletion = () => {};

  // This functions is the one that interacts with
  // the "backend". Gets the connections and if there
  // are some that match the filter updates the last
  // received id and the property has more for the
  // infinite scroll
  const getFilteredCredentialSummaries = userId => {
    const { id } = credentialSummaries.length
      ? credentialSummaries[credentialSummaries.length - 1]
      : {};

    getConnectionsPaginated(userId, id)
      .then(summariesResponse => {
        if (!summariesResponse.length) {
          setHasMore(false);
          return;
        }

        const parsedSummaries = summariesResponse.map(
          ({ connectionid, created, participantinfo: { holder } }) => ({
            id: connectionid,
            date: created,
            user: holder
          })
        );
        setCredentialSummaries(credentialSummaries.concat(parsedSummaries));
        // This gets the id of the last item of the array of credential
        // summaries sent from the backend and saves it to use in future
        // calls
        setLastId(summariesResponse[summariesResponse.length - 1].connectionid);
      })
      .catch(error => {
        Logger.error(error);
        message.error(t('errors.errorGetting', { model: 'CredentialSummaries' }));
      });
  };

  // This gets the connections when the component mounts
  useEffect(() => {
    getFilteredCredentialSummaries();
  }, []);

  // This hooks listens for the filter changes, cleans the
  // id and list so it doesn't append to the previous list
  // or skips registers that must be listed
  useEffect(() => {
    setCredentialSummaries([]);
    setHasMore(true);
  }, [date, name]);

  // This hook listens for the id and connection cleanup
  // and then gets the connections
  useEffect(() => {
    if (!lastId && hasMore && !credentialSummaries.length) getFilteredCredentialSummaries();
  }, [lastId, hasMore, credentialSummaries]);

  const credentialSummariesProps = {
    onPageChange: getFilteredCredentialSummaries,
    credentialSummaries,
    setDate,
    setName,
    handleCredentialSummaryDeletion,
    getFilteredCredentialSummaries,
    hasMore
  };

  return <CredentialSummaries {...credentialSummariesProps} />;
};

CredentialSummaryController.propTypes = {
  api: PropTypes.shape({ getConnections: PropTypes.func, getConnectionsPaginated: PropTypes.func })
    .isRequired
};

export default withApi(CredentialSummaryController);
