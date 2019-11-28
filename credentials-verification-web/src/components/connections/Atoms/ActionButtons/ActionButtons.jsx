import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const ActionButtons = ({ id, showQRButton, inviteHolder, isIssuer }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {showQRButton && (
        <CustomButton
          buttonProps={{
            onClick: () => inviteHolder(id),
            className: 'theme-link'
          }}
          buttonText={t('connections.table.columns.invite')}
        />
      )}
      <CustomButton
        buttonProps={{ className: 'theme-link' }}
        buttonText={t('connections.table.columns.delete')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link'
        }}
        buttonText={t(`connections.table.columns.${isIssuer ? 'view' : 'viewCredentials'}`)}
      />
    </div>
  );
};

ActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  showQRButton: PropTypes.bool.isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired
};

export default ActionButtons;
