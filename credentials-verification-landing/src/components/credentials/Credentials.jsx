import React from 'react';
import PropTypes from 'prop-types';
import { Icon, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import SplittedPage from './Organisms/SplittedPage/SplittedPage';
import CredentialsList from './Organisms/CredentialList/CredentialsList';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { LEFT } from '../../helpers/constants';

const Credentials = ({
  redirector: { redirectToLanding, redirectToContact },
  changeCurrentCredential,
  getStep,
  availableCredential,
  showContactButton
}) => {
  const { t } = useTranslation();

  const credentialsRenderer = () => (
    <CredentialsList
      changeCurrentCredential={changeCurrentCredential}
      availableCredential={availableCredential}
      showContactButton={showContactButton}
      toContactForm={redirectToContact}
    />
  );

  return (
    <div className="CredentialContainer">
      <div className="LogoContent">
        <img src="images/atala-logo.svg" alt={t('atalaLogo')} />
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
        <SplittedPage renderLeft={credentialsRenderer} renderRight={getStep} />
      </div>
    </div>
  );
};

Credentials.propTypes = {
  availableCredential: PropTypes.number.isRequired,
  changeCurrentCredential: PropTypes.func.isRequired,
  getStep: PropTypes.func.isRequired,
  showContactButton: PropTypes.bool.isRequired,
  redirector: PropTypes.shape({
    redirectToLanding: PropTypes.func.isRequired,
    redirectToContact: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(Credentials);
