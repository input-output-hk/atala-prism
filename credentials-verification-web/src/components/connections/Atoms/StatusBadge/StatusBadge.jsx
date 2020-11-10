import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CONNECTION_STATUSES_TRANSLATOR } from '../../../../helpers/constants';

import './_style.scss';

const StatusBadge = ({ status }) => {
  const { t } = useTranslation();
  const connectionStatus = CONNECTION_STATUSES_TRANSLATOR[status];

  return (
    <div className={`Label ${connectionStatus}`}>
      <p>{t(`contacts.status.${connectionStatus}`)}</p>
    </div>
  );
};

StatusBadge.propTypes = {
  status: PropTypes.number.isRequired
};

export default StatusBadge;
