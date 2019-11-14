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
  const [subjectCount, setSubjectCount] = useState(0);

  // This is used to paginate
  const [offset, setOffset] = useState(0);

  // This should have the backend call
  useEffect(() => {
    api
      .getHolders({ identityNumber, name, status, pageSize: HOLDER_PAGE_SIZE, offset })
      .then(({ holders, holderCount }) => {
        const holdersWithKey = holders.map(holder => Object.assign({}, holder, { key: holder.id }));
        setSubjects(holdersWithKey);
        setSubjectCount(holderCount);
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

  const inviteHolder = cb => api.geConnectionToken(null, cb);

  const tableProps = { subjects, subjectCount, offset, setOffset };
  const filterProps = {
    identityNumber,
    setIdentityNumber: value => updateFilter(value, setIdentityNumber),
    name,
    setName: value => updateFilter(value, setName),
    status,
    setStatus: value => updateFilter(value, setStatus)
  };

  return (
    <Connections tableProps={tableProps} filterProps={filterProps} inviteHolder={inviteHolder} />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
