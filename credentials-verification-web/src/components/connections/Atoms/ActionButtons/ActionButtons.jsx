import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { studentShape } from '../../../../helpers/propShapes';
import { CONNECTION_STATUSES, INDIVIDUAL_STATUSES } from '../../../../helpers/constants';

const showQR = ({ status }) => {
  const invitationMissing = [
    CONNECTION_STATUSES.invitationMissing,
    CONNECTION_STATUSES.connectionMissing
  ].includes(status);

  const createdOrRevoked = [INDIVIDUAL_STATUSES.created, INDIVIDUAL_STATUSES.revoked].includes(
    status
  );

  return invitationMissing || createdOrRevoked;
};

const ActionButtons = ({ inviteHolder, isIssuer, viewConnectionDetail, holder }) => {
  const { t } = useTranslation();
  const { id } = holder;

  const showQRButton = showQR(holder);

  return (
    <div className="ControlButtons">
      {!(isIssuer() || showQRButton) && (
        <CustomButton
          buttonProps={{
            className: 'theme-link',
            onClick: () => viewConnectionDetail(holder)
          }}
          buttonText={t('connections.table.columns.viewCredentials')}
        />
      )}
      {/* TODO uncomment when this work*/}
      {/*<CustomButton*/}
      {/*  buttonProps={{ className: 'theme-link', disabled: true }}*/}
      {/*  buttonText={t('connections.table.columns.delete')}*/}
      {/*/>*/}
      {showQRButton && (
        <CustomButton
          buttonProps={{
            onClick: () => inviteHolder(id),
            className: 'theme-link'
          }}
          buttonText={t('connections.table.columns.invite')}
        />
      )}
    </div>
  );
};

ActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  viewConnectionDetail: PropTypes.func.isRequired,
  holder: PropTypes.shape(studentShape).isRequired
};

export default ActionButtons;
