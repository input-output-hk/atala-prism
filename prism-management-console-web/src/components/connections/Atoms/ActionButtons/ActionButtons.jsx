import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { studentShape } from '../../../../helpers/propShapes';
import { CONNECTION_STATUSES } from '../../../../helpers/constants';

const showQR = ({ connectionStatus }) =>
  [
    CONNECTION_STATUSES.statusInvitationMissing,
    CONNECTION_STATUSES.statusConnectionMissing
  ].includes(connectionStatus);

const ActionButtons = ({ inviteContact, viewContactDetail, contact }) => {
  const { t } = useTranslation();
  const { contactId } = contact;

  const showQRButton = showQR(contact);

  return (
    <div className="ControlButtons">
      {showQRButton && (
        <CustomButton
          buttonProps={{
            onClick: () => inviteContact(contactId),
            className: 'theme-link'
          }}
          buttonText={t('contacts.table.columns.invite')}
        />
      )}
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('contacts.table.columns.delete')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('contacts.table.columns.edit')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => viewContactDetail(contactId)
        }}
        buttonText={t('contacts.table.columns.view')}
      />
    </div>
  );
};

ActionButtons.propTypes = {
  inviteContact: PropTypes.func.isRequired,
  viewContactDetail: PropTypes.func.isRequired,
  contact: PropTypes.shape(studentShape).isRequired
};

export default ActionButtons;
