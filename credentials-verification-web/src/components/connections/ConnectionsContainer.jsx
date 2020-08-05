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
  const [filteredSubjects, setFilteredSubjects] = useState([]);
  const [hasMore, setHasMore] = useState(true);

  const getConnections = ({
    pageSize,
    lastId,
    _name,
    _status,
    _email,
    isRefresh,
    oldConnections = []
  }) => {
    const getIndividuals = api.getIndividuals(api.wallet.isIssuer());

    return (hasMore || isRefresh ? getIndividuals(pageSize, lastId) : Promise.resolve([]))
      .then(connections => {
        if (connections.length < HOLDER_PAGE_SIZE) {
          setHasMore(false);
        } else {
          Logger.warn(
            'There were more rows than expected. Frontend-only filters will yield incomplete results'
          );
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

        const updatedConnections = oldConnections.concat(connectionsWithKey);

        const filteredConnections = updatedConnections.filter(it => {
          const caseInsensitiveMatch = (str1 = '', str2 = '') =>
            str1.toLowerCase().includes(str2.toLowerCase());

          const matchesName = caseInsensitiveMatch(it.fullname, _name);
          const matchesEmail = caseInsensitiveMatch(it.email, _email);
          // 0 is a valid status so it's not possible to check for !_status
          const matchesStatus = [undefined, '', it.status].includes(_status);

          return matchesStatus && matchesName && matchesEmail;
        });

        setSubjects(updatedConnections);
        setFilteredSubjects(filteredConnections);
      })
      .catch(error => {
        Logger.error('[Connections.getConnections] Error while getting connections', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }));
      });
  };

  const refreshConnections = () => getConnections({ pageSize: subjects.length, isRefresh: true });

  const handleHoldersRequest = (_name, _email, _status) => {
    const { id } = getLastArrayElementOrEmpty(subjects);

    return getConnections({
      pageSize: HOLDER_PAGE_SIZE,
      lastId: id,
      _name,
      _status,
      _email,
      oldConnections: subjects
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
    subjects: filteredSubjects,
    hasMore,
    getCredentials
  };

  const isIssuer = () => api.wallet.isIssuer();

  return (
    <Connections
      tableProps={tableProps}
      handleHoldersRequest={handleHoldersRequest}
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
