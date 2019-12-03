import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const ActionButtons = ({ showQRButton, inviteHolder, isIssuer, setHolder, holder }) => {
  const { t } = useTranslation();

  const { admissionDate, avatar, id, name, transactions } = holder;

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
          className: 'theme-link',
          onClick: () => {
            const formattedHolder = {
              user: { icon: avatar, name },
              transactions,
              date: admissionDate
            };
            setHolder(formattedHolder);
          }
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
