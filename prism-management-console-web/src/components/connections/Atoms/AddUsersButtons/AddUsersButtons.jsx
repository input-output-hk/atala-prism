import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';

const AddUsersButtons = ({ redirector: { redirectToImportContacts } }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick: redirectToImportContacts
        }}
        buttonText={t('contacts.buttons.import')}
        icon={<PlusOutlined />}
      />
    </div>
  );
};

AddUsersButtons.propTypes = {
  redirector: PropTypes.shape({
    redirectToImportContacts: PropTypes.func
  }).isRequired
};

export default withRedirector(AddUsersButtons);
