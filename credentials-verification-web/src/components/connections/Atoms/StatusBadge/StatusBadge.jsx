import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import __ from 'lodash';
import { CONNECTION_STATUSES } from '../../../../helpers/constants';

import './_style.scss';

const StatusBadge = ({ status = 'invitationMissing' }) => {
  const { t } = useTranslation();
  const classname = `Label ${status}`;

  return (
    <div className={classname}>
      <p>{t(`connections.status.${status}`)}</p>
    </div>
  );
};

StatusBadge.propTypes = {
  status: PropTypes.oneOf(__.values(CONNECTION_STATUSES)).isRequired
};

export default StatusBadge;
