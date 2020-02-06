import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { connectionStatusesKeysShape } from '../../../../helpers/propShapes';
import { USER_ROLE, ISSUER } from '../../../../helpers/constants';

import './_style.scss';

const StatusBadge = ({ status }) => {
  const { t } = useTranslation();
  const classname = `Label ${status}`;

  const role = localStorage.getItem(USER_ROLE);
  const defaultStatus = role === ISSUER ? 'invitationMissing' : 'created';
  const lowerCaseRole = role.toLowerCase();

  return (
    <div className={classname}>
      <p>{t(`connections.status.${lowerCaseRole}.${status || defaultStatus}`)}</p>
    </div>
  );
};

StatusBadge.propTypes = {
  status: PropTypes.oneOf(connectionStatusesKeysShape).isRequired
};

export default StatusBadge;
