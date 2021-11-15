import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import { Icon, Row } from 'antd';
import PropTypes from 'prop-types';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../../../components/customButton/CustomButton';
import { LEFT } from '../../../helpers/constants';

import './_style.scss';

const UserInfo = ({ redirector: { redirectToLanding }, getStep }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialContainer">
      <div className="LogoContent">
        <img src="/images/logo-atala-prism.svg" alt={t('atalaLogo')} />
      </div>
      <div className="CredentialStepContent">
        <Row>
          <CustomButton
            buttonProps={{
              onClick: redirectToLanding,
              className: 'theme-link'
            }}
            icon={{ icon: <Icon type="left" />, side: LEFT }}
            buttonText={t('credential.backHome')}
          />
        </Row>
        {getStep()}
      </div>
      <div className="LogoContent">
        <img src="/images/logo-cardano.svg" alt={t('atalaLogo')} />
      </div>
    </div>
  );
};

UserInfo.propTypes = {
  redirector: PropTypes.shape({ redirectToLanding: PropTypes.func }).isRequired,
  getStep: PropTypes.func.isRequired
};

export default withRedirector(UserInfo);
