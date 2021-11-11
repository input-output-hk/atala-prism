import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Icon, Row } from 'antd';
import firebase from 'gatsby-plugin-firebase';
import SplittedPage from './Organisms/SplittedPage/SplittedPage';
import CredentialsList from './Organisms/CredentialList/CredentialsList';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../../../components/customButton/CustomButton';
import { LEFT, RESET_DEMO_EVENT } from '../../../helpers/constants';
import buttonReset from '../../images/icon-reset.svg';
import { UserContext } from '../providers/userContext';

const Credentials = ({
  redirector: { redirectToLanding, redirectToContact },
  changeCurrentCredential,
  getStep,
  availableCredential,
  showContactButton,
  showCongrats
}) => {
  const { setUser } = useContext(UserContext);

  const credentialsRenderer = () => (
    <CredentialsList
      changeCurrentCredential={changeCurrentCredential}
      availableCredential={availableCredential}
      showContactButton={showContactButton}
      toContactForm={redirectToContact}
      showCongrats={showCongrats}
    />
  );

  const handleReset = () => {
    firebase.analytics().logEvent(RESET_DEMO_EVENT);
    setUser(null);
  };

  return (
    <div className="CredentialContainer">
      <div className="CredentialStepContent">
        <Row className="ControlButtons">
          <button type="button" onClick={handleReset}>
            <img src={buttonReset} alt="ButtonReset" className="ButtonReset" />
          </button>
          <hr />
          <CustomButton
            buttonProps={{
              onClick: redirectToLanding,
              className: 'theme-primary'
            }}
            icon={{ icon: <Icon type="close" />, side: LEFT }}
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
  showCongrats: PropTypes.bool.isRequired,
  redirector: PropTypes.shape({
    redirectToLanding: PropTypes.func.isRequired,
    redirectToContact: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(Credentials);
