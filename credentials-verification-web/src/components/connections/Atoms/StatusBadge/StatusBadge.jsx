import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { connectionStatusesKeysShape } from '../../../../helpers/propShapes';
import { ISSUER } from '../../../../helpers/constants';

import './_style.scss';
import { useSession } from '../../../providers/SessionContext';

const StatusBadge = ({ status }) => {
  const { t } = useTranslation();
  const classname = `Label ${status}`;

  const { session } = useSession();
  const role = session.userRole;

  const defaultStatus = role === ISSUER ? 'invitationMissing' : 'created';
  const lowerCaseRole = role.toLowerCase();

  return (
    <div className={classname}>
      <p>{t(`contacts.status.${lowerCaseRole}.${status || defaultStatus}`)}</p>
    </div>
  );
};

StatusBadge.propTypes = {
  status: PropTypes.oneOf(connectionStatusesKeysShape).isRequired
};

export default StatusBadge;
