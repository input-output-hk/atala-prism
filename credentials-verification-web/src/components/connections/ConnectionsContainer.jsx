import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Connections from './Connections';
import Logger from '../../helpers/Logger';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';
import { withApi } from '../providers/witApi';

const ConnectionsContainer = ({ api }) => {
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
  const [subjectCount] = useState(HOLDER_PAGE_SIZE);

  // This is used to paginate
  const [offset, setOffset] = useState(0);

  // This should have the backend call
  useEffect(() => {
    api
      .getStudents(HOLDER_PAGE_SIZE)
      .then(holders => {
        const holdersWithKey = holders.map(holder => Object.assign({}, holder, { key: holder.id }));
        setSubjects(holdersWithKey);
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

  const inviteHolder = studentId => api.generateConnectionToken(undefined, studentId);

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
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
