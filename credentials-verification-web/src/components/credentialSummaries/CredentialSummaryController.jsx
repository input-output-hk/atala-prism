import React, { useState, useEffect } from 'react';
import moment from 'moment';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import CredentialSummaries from './CredentialSummaries';
import { withApi } from '../providers/witApi';

const CredentialSummaryController = ({ api: { getCredentialSummaries } }) => {
  const { t } = useTranslation();

  const [credentialSummaries, setCredentialSummaries] = useState([]);
  const [count, setCount] = useState(0);
  const [offset, setOffset] = useState(0);
  const [date, setDate] = useState();
  const [name, setName] = useState('');

  const handleCredentialSummaryDeletion = () => {};

  useEffect(() => {
    const filterDateAsUnix = date ? moment(date).unix() : 0;

    getCredentialSummaries({ date: filterDateAsUnix, name, offset })
      .then(({ credentialSummaries: credentialSummaryResponse, credentialSummaryCount }) => {
        setCredentialSummaries(credentialSummaryResponse);
        setCount(credentialSummaryCount);
      })
      .catch(() => message.error(t('errors.errorGetting', 'CredentialSummaries')));
  }, [date, name, offset]);

  const credentialSummariesProps = {
    credentialSummaries,
    count,
    offset,
    setOffset,
    setDate,
    setName,
    handleCredentialSummaryDeletion
  };

  return <CredentialSummaries {...credentialSummariesProps} />;
};

export default withApi(CredentialSummaryController);
