import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { studentShape } from '../../../../helpers/propShapes';

const ActionButtons = ({
  showQRButton,
  inviteHolder,
  isIssuer,
  setHolder,
  holder,
  getCredentials
}) => {
  const { t } = useTranslation();

  const { admissiondate, avatar, id, fullname, connectionid } = holder;

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
              user: { icon: avatar, name: fullname },
              transactions: getCredentials(connectionid),// TODO pass func to getCredentials //in case verifier
              // transactions: [{icon: avatar, type: 'type', date: new Date(), setConnectionInfo: () => true}],// TODO pass func to getCredentials //in case verifier
              date: admissiondate
            };
            setHolder(formattedHolder);
          }
        }}
        buttonText={t(`connections.table.columns.${isIssuer() ? 'view' : 'viewCredentials'}`)}
      />
    </div>
  );
};

ActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  showQRButton: PropTypes.bool.isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  setHolder: PropTypes.func.isRequired,
  holder: PropTypes.shape(studentShape).isRequired,
  getCredentials: PropTypes.func.isRequired
};

export default ActionButtons;
