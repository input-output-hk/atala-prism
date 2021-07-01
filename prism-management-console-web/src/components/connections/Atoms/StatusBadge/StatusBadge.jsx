import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  CONNECTION_STATUSES_TRANSLATOR,
  CREDENTIAL_STATUSES_TRANSLATOR,
  CONTACT_STATUS,
  CREDENTIAL_STATUS
} from '../../../../helpers/constants';

import './_style.scss';

const StatusBadge = ({ status, useCase }) => {
  const { t } = useTranslation();

  const getStatusKey = {
    [CONTACT_STATUS]: CONNECTION_STATUSES_TRANSLATOR[status],
    [CREDENTIAL_STATUS]: CREDENTIAL_STATUSES_TRANSLATOR[status]
  };

  const statusKey = getStatusKey[useCase];

  return (
    <div className={`Label ${statusKey || 'undefined'}`}>
      <p>{t(`${useCase}.status.${statusKey || 'undefined'}`)}</p>
    </div>
  );
};

StatusBadge.defaultProps = {
  useCase: 'contacts'
};

StatusBadge.propTypes = {
  status: PropTypes.number.isRequired,
  useCase: PropTypes.oneOf([CONTACT_STATUS, CREDENTIAL_STATUS])
};

export default StatusBadge;
