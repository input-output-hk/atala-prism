import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import DocumentAcceptation from './Molecules/DocumentAcceptation/DocumentAcceptation';
import Registration from './Registration';
import { withApi } from '../providers/withApi';
import DownloadWallet from './Molecules/DownloadWallet/DownloadWallet';
import Congratulations from './Atoms/Congratulations/Congratulations';
import { LOCKED, UNLOCKED } from '../../helpers/constants';

const TERMS_AND_CONDITIONS_STEP = 0;
const PRIVACY_POLICY_STEP = 1;
const DOWNLOAD_WALLET_STEP = 2;
const STEP_QUANTITY = 3;

const RegistrationContainer = ({ api }) => {
  const { t } = useTranslation();

  const { getTermsAndConditions, getPrivacyPolicy } = api;

  const [currentStep, setCurrentStep] = useState(TERMS_AND_CONDITIONS_STEP);
  const [termsAndConditions, setTermsAndConditions] = useState('');
  const [privacyPolicy, setPrivacyPolicy] = useState('');
  const [walletError, setWalletError] = useState(null);
  const [disableNextButton, setDisableNextButton] = useState(false);

  useEffect(() => {
    getTermsAndConditions()
      .then(doc => setTermsAndConditions(doc))
      .catch(() =>
        message.error(
          t('registration.errorGetting', { document: t('registration.termsAndConditions') })
        )
      );
    getPrivacyPolicy()
      .then(doc => setPrivacyPolicy(doc))
      .catch(() =>
        message.error(t('registration.errorGetting', { document: t('registration.privacyPolicy') }))
      );
  }, []);

  const nextStep = () => setCurrentStep(currentStep + 1);

  const nextIfUserRegistered = async () => {
    const { wallet } = api;

    const isUnlocked = session => session?.sessionState === UNLOCKED;
    const isLocked = session => session?.sessionState === LOCKED;

    setDisableNextButton(true);
    const session = await wallet.getSession();
    setDisableNextButton(false);

    if (isUnlocked(session)) {
      setWalletError(null);
      nextStep();
    } else if (isLocked(session)) {
      setWalletError(new Error('errors.walletNotRegistered'));
    } else {
      setWalletError(new Error('errors.walletNotRunning'));
    }
  };

  const nextFunction = () => {
    switch (currentStep) {
      case TERMS_AND_CONDITIONS_STEP:
      case PRIVACY_POLICY_STEP:
        return nextStep;
      case DOWNLOAD_WALLET_STEP:
        return nextIfUserRegistered;
      default:
        return nextStep;
    }
  };

  const getContent = () => {
    switch (currentStep) {
      case TERMS_AND_CONDITIONS_STEP:
        return (
          <DocumentAcceptation
            title="registration.termsAndConditions"
            lastUpdated={moment()}
            content={termsAndConditions}
          />
        );
      case PRIVACY_POLICY_STEP:
        return (
          <DocumentAcceptation
            title="registration.privacyPolicy"
            lastUpdated={moment()}
            content={privacyPolicy}
          />
        );
      case DOWNLOAD_WALLET_STEP:
        return <DownloadWallet walletError={walletError} />;
      default:
        return <Congratulations />;
    }
  };

  const requiresAgreement = () => {
    const stepsWithAgreement = [TERMS_AND_CONDITIONS_STEP, PRIVACY_POLICY_STEP];

    return stepsWithAgreement.includes(currentStep);
  };

  const footerProps = {
    next: nextFunction(),
    previous: currentStep ? () => setCurrentStep(currentStep - 1) : null,
    requiresAgreement: requiresAgreement(),
    disabled: disableNextButton
  };

  return (
    <Registration
      renderContent={() => getContent(currentStep)}
      footerProps={footerProps}
      renderFooter={currentStep < STEP_QUANTITY}
    />
  );
};

RegistrationContainer.propTypes = {
  api: PropTypes.shape({
    getTermsAndConditions: PropTypes.func.isRequired,
    getPrivacyPolicy: PropTypes.func.isRequired,
    wallet: PropTypes.shape({
      getSession: PropTypes.func.isRequired
    }).isRequired,
    connector: PropTypes.shape({
      registerUser: PropTypes.func.isRequired
    }).isRequired
  }).isRequired
};

export default withApi(RegistrationContainer);
