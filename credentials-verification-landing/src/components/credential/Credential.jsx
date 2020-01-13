import React from 'react';
import { useTranslation } from 'react-i18next';
import { Icon, Row } from 'antd';
import PropTypes from 'prop-types';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const Credential = ({ redirector: { redirectToLanding }, getStep }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialContainer">
      <div className="CredentialContent">
        <Row>
          <CustomButton
            buttonProps={{
              onClick: redirectToLanding,
              className: 'theme-link'
            }}
            icon={<Icon type="arrow-left" />}
            buttonText={t('credential.backHome')}
          />
        </Row>
        {getStep()}
      </div>
    </div>
  );
};

Credential.propTypes = {
  redirector: PropTypes.shape({ redirectToLanding: PropTypes.func }).isRequired,
  getStep: PropTypes.func.isRequired
};

export default withRedirector(Credential);
