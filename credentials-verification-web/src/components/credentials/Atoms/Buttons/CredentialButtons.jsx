import React from 'react';
import { Icon } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const CredentialsButtons = ({ redirector: { redirectToNewCredential } }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'buttonCredential theme-outline',
          onClick: redirectToNewCredential
        }}
        buttonText={t('credentials.actions.createCredential')}
        icon={<Icon type="plus" />}
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
