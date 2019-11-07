import React, { useState, useEffect } from 'react';
import moment from 'moment';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import Connections from './Connections';
import { withApi } from '../providers/witApi';
import { dateAsUnix } from '../../helpers/formatters';

const ConnectionsController = ({ api: { getConnections } }) => {
  const { t } = useTranslation();

  const [connections, setConnections] = useState([]);
  const [count, setCount] = useState(0);
  const [offset, setOffset] = useState(0);
  const [date, setDate] = useState();
  const [name, setName] = useState('');

  const handleConnectionDeletion = () => {};

  useEffect(() => {
    const filterDateAsUnix = dateAsUnix(date);

    getConnections({ date: filterDateAsUnix, name, offset })
      .then(({ connections: connectionResponse, connectionsCount }) => {
        setConnections(connectionResponse);
        setCount(connectionsCount);
      })
      .catch(() => message.error(t('errors.errorGetting', 'Connections')));
  }, [date, name, offset]);

  const connectionProps = {
    connections,
    count,
    offset,
    setOffset,
    setDate,
    setName,
    handleConnectionDeletion
  };

  return <Connections {...connectionProps} />;
};

export default withApi(ConnectionsController);
