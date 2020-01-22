import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { studentShape } from '../../../../helpers/propShapes';

const ActionButtons = ({ showQRButton, inviteHolder, isIssuer, viewConnectionDetail, holder }) => {
  const { t } = useTranslation();
  const { id } = holder;

  const issuer = isIssuer();

  return (
    <div className="ControlButtons">
      {!issuer && (
        <CustomButton
          buttonProps={{
            className: 'theme-link',
            onClick: () => viewConnectionDetail(holder)
          }}
          buttonText={t(`connections.table.columns.${issuer ? 'view' : 'viewCredentials'}`)}
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
  showQRButton: PropTypes.bool.isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  viewConnectionDetail: PropTypes.func.isRequired,
  holder: PropTypes.shape(studentShape).isRequired
};

export default ActionButtons;
