import React from 'react';
import { PlusOutlined } from '@ant-design/icons';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const CredentialsButtons = ({ redirector: { redirectToNewCredential } }) => {
  const { t } = useTranslation();

  return (
    <div className="MainOption">
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick: redirectToNewCredential
        }}
        buttonText={t('credentials.actions.createCredential')}
        icon={<PlusOutlined />}
      />
    </div>
  );
};

CredentialsButtons.propTypes = {
  redirector: PropTypes.shape({
    redirectToNewCredential: PropTypes.func
  }).isRequired
};

export default withRedirector(CredentialsButtons);
