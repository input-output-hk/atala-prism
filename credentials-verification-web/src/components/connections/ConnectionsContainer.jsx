import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Connections from './Connections';
import Logger from '../../helpers/Logger';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';
import { withApi } from '../providers/withApi';
import { withRedirector } from '../providers/withRedirector';

const ConnectionsContainer = ({ api, redirector: { redirectToBulkImport } }) => {
  const { t } = useTranslation();

  // These are the values used to filter the holders/subjects
  const [identityNumber, setIdentityNumber] = useState('');
  const [name, setName] = useState('');
  const [status, setStatus] = useState(t(''));

  // These are the holders/subjects returned from the "backend"
  const [subjects, setSubjects] = useState([]);

  // This is the amount of holders/subjects by the sent query
  // Until the infinite pagination is in all of the tables
  // this count will be hardcoded since there is no way of
  // knowing the total amount of subjects
  const [subjectCount, setSubjectCount] = useState(0);

  // This is used to paginate
  const [offset, setOffset] = useState(0);

  useEffect(() => {
    const getIndividuals = api.getIndividuals(api.isIssuer());

    getIndividuals(HOLDER_PAGE_SIZE)
      .then(holders => {
        const holdersWithKey = holders.map(
          ({ status: holderStatus, connectionstatus, id: holderId, individualid, ...rest }) => {
            const id = holderId || individualid;
            const indivStatus = holderStatus || connectionstatus;

            return Object.assign({}, rest, {
              key: id,
              status: indivStatus,
              id
            });
          }
        );
        setSubjects(holdersWithKey);
        setSubjectCount(holdersWithKey.length);
      })
      .catch(error => {
        Logger.error('[Connections.getHolders] Error while getting holders', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }), 1);
      });
  }, [identityNumber, name, status, offset]);

  const updateFilter = (value, setField) => {
    setOffset(0);
    setField(value);
  };

  const inviteHolder = studentId => {
    const generateConnectionToken = api.generateConnectionToken(api.isIssuer());

    return generateConnectionToken(studentId);
  };

  const tableProps = {
    subjects,
    subjectCount,
    offset,
    setOffset,
    getCredentials: api.getMessagesForConnection
  };
  const filterProps = {
    identityNumber,
    setIdentityNumber: value => updateFilter(value, setIdentityNumber),
    name,
    setName: value => updateFilter(value, setName),
    status,
    setStatus: value => updateFilter(value, setStatus)
  };

  return (
    <Connections
      tableProps={tableProps}
      filterProps={filterProps}
      inviteHolder={inviteHolder}
      isIssuer={api.isIssuer}
      redirectToBulkImport={redirectToBulkImport}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(withRedirector(ConnectionsContainer));
