import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const AddUsersButton = ({ onClick }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick,
          icon: <PlusOutlined />
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
