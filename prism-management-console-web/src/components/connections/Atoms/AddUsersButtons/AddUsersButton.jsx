import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const AddUsersButton = ({ onClick }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick
        }}
        buttonText={t('contacts.buttons.import')}
      />
    </div>
  );
};

AddUsersButton.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default AddUsersButton;
