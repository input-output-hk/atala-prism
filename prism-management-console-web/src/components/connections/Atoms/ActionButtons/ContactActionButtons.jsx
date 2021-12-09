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

const ContactActionButtons = ({ inviteContact, viewContactDetail, contact }) => {
  const { t } = useTranslation();
  const { contactId } = contact;

  const showQRButton = showQR(contact);

  const actions = [
    { name: 'invite', call: () => inviteContact(contactId), hideCondition: !showQRButton },
    { name: 'delete' },
    { name: 'edit', call: () => viewContactDetail(contactId, true) },
    { name: 'view', call: () => viewContactDetail(contactId) }
  ];

  const defaultAction = () => message.warn(t('errors.notImplementedYet'));

  return (
    <div className="ControlButtons">
      {actions.map(
        a =>
          !a.hideCondition && (
            <CustomButton
              key={a.name}
              buttonProps={{
                className: 'theme-link',
                onClick: a.call || defaultAction
              }}
              buttonText={t(`contacts.table.columns.${a.name}`)}
            />
          )
      )}
    </div>
  );
};

ContactActionButtons.propTypes = {
  inviteContact: PropTypes.func.isRequired,
  viewContactDetail: PropTypes.func.isRequired,
  contact: PropTypes.shape(studentShape).isRequired
};

export default ContactActionButtons;
