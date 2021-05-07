import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { studentShape } from '../../helpers/propShapes';

const ActionButtons = ({ contact, onDelete }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => onDelete(contact.contactId)
        }}
        buttonText={t('contacts.table.columns.remove')}
      />
    </div>
  );
};

ActionButtons.defaultProps = {};

ActionButtons.propTypes = {
  onDelete: PropTypes.func.isRequired,
  contact: PropTypes.shape(studentShape).isRequired
};

export default ActionButtons;
