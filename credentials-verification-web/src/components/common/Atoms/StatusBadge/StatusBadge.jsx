import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const StatusBadge = ({ status }) => {
  const { t } = useTranslation();
  const classname = 'Label ' + status;

  return (
    <div className={classname}>
      <p>{t(`recipients.status.${status}`)}</p>
    </div>
  );
};

StatusBadge.propTypes = {
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED', 'PENDING_INVITATION']).isRequired
};

export default StatusBadge;
