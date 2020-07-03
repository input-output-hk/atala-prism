import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Connections from './Connections';
import Logger from '../../helpers/Logger';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';
import { withApi } from '../providers/withApi';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';

const ConnectionsContainer = ({ api }) => {
  const { t } = useTranslation();

  const [subjects, setSubjects] = useState([]);
  const [noConnections, setNoConnections] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  const getConnections = ({ pageSize, lastId, _name, _status, isRefresh, oldConnections }) => {
    const getIndividuals = api.getIndividuals(api.wallet.isIssuer());

    return getIndividuals(pageSize, lastId)
      .then(connections => {
        if (!isRefresh) {
          const noFilters = !(_name || _status);
          const showNoConnections = !lastId && noFilters;
          setNoConnections(showNoConnections);

          if (!connections.length) setHasMore(false);
        }

        const connectionsWithKey = connections.map(
          ({ status: holderStatus, connectionstatus, id: holderId, individualid, ...rest }) => {
            const existingId = holderId || individualid;
            const indivStatus = holderStatus !== undefined ? holderStatus : connectionstatus;

            return Object.assign({}, rest, {
              key: existingId,
              status: indivStatus,
              id: existingId
            });
          }
        );

        const updatedConnections = oldConnections
          ? oldConnections.concat(connectionsWithKey)
          : connectionsWithKey;

        setSubjects(updatedConnections);
      })
      .catch(error => {
        Logger.error('[Connections.getConnections] Error while getting connections', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }));
      });
  };

  const refreshConnections = () => getConnections({ pageSize: subjects.length, isRefresh: true });

  const handleHoldersRequest = (oldConnections = subjects, _name, _status) => {
    const { id } = getLastArrayElementOrEmpty(oldConnections);

    return getConnections({
      pageSize: HOLDER_PAGE_SIZE,
      lastId: id,
      _name,
      _status,
      oldConnections
    });
  };

  const inviteHolder = studentId => {
    const generateConnectionToken = api.generateConnectionToken(api.wallet.isIssuer());

    return generateConnectionToken(studentId);
  };

  useEffect(() => {
    if (!subjects.length) handleHoldersRequest();
  }, []);

  // Wrapper to preserve 'this' context
  const getCredentials = connectionId => api.connector.getMessagesForConnection(connectionId);

  const tableProps = {
    subjects,
    hasMore,
    getCredentials
  };

  const isIssuer = () => api.wallet.isIssuer();

  return (
    <Connections
      tableProps={tableProps}
      handleHoldersRequest={handleHoldersRequest}
      noConnections={noConnections}
      inviteHolder={inviteHolder}
      isIssuer={isIssuer}
      refreshConnections={refreshConnections}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
