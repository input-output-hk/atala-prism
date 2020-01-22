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

  const handleHoldersRequest = (oldSubjects = subjects, _name, _status) => {
    const { id } = getLastArrayElementOrEmpty(oldSubjects);

    const getIndividuals = api.getIndividuals(api.isIssuer());

    return getIndividuals(HOLDER_PAGE_SIZE, id)
      .then(holders => {
        const noFilters = !(_name || _status);
        const showNoConnections = !id && noFilters;
        setNoConnections(showNoConnections);

        if (!holders.length) {
          setHasMore(false);
          return;
        }

        const holdersWithKey = holders.map(
          ({ status: holderStatus, connectionstatus, id: holderId, individualid, ...rest }) => {
            const existingId = holderId || individualid;
            const indivStatus = holderStatus || connectionstatus;

            return Object.assign({}, rest, {
              key: existingId,
              status: indivStatus,
              id: existingId
            });
          }
        );

        const newSubjects = oldSubjects.concat(holdersWithKey);
        setSubjects(newSubjects);
      })
      .catch(error => {
        Logger.error('[Connections.getHolders] Error while getting holders', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }), 1);
      });
  };

  const inviteHolder = studentId => {
    const generateConnectionToken = api.generateConnectionToken(api.isIssuer());

    return generateConnectionToken(studentId);
  };

  useEffect(() => {
    if (!subjects.length) handleHoldersRequest();
  }, []);

  const tableProps = {
    subjects,
    hasMore,
    getCredentials: api.getMessagesForConnection
  };

  return (
    <Connections
      tableProps={tableProps}
      handleHoldersRequest={handleHoldersRequest}
      noConnections={noConnections}
      inviteHolder={inviteHolder}
      isIssuer={api.isIssuer}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
