import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import FinishInfo from './Molecules/FinishInfo/FinishInfo';
import CreatedCredential from './Organisms/CreatedCredential/CreatedCredential';
import QRCard from './Molecules/QRCard/QRCard';
import ScanQRInfo from './Molecules/ScanQRInfo/ScanQRInfo';
import IntroductionInformation from './Organisms/IntroductionInformation/IntroductionInformation';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { CONNECTION_ACCEPTED, SUBJECT_STATUSES } from '../../helpers/constants';
import Credentials from './Credentials';
import SplittedPageInside from './Organisms/SplittedPageInside/SplittedPageInside';
import WhatYouNeed from './Molecules/WhatYouNeed/WhatYouNeed';

// Credentials steps
export const INTRODUCTION_STEP = 0;
export const QR_STEP = 1;
// export const SEND_CREDENTIAL_STEP = 2;
export const SUCCESS_STEP = 2; // 3;

const CredentialsContainer = ({ api: { getConnectionToken, startSubjectStatusStream } }) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(0);
  const [token, setToken] = useState();
  const [enabledCredentialToRequest, setEnabledCredentialToRequest] = useState(0);
  const [currentCredential, setCurrentCredential] = useState(0);
  const [connectionStatus, setConnectionStatus] = useState();

  useEffect(() => {
    setCurrentStep(0);
    setToken();
    setConnectionStatus(undefined);
  }, [currentCredential]);

  useEffect(() => {
    const isIntroductionStep = currentStep === INTRODUCTION_STEP;
    if (token && isIntroductionStep) setCurrentStep(currentStep + 1);
  }, [token, currentStep]);

  useEffect(() => {
    const isSuccessStep = currentStep === SUCCESS_STEP;
    if (isSuccessStep) setEnabledCredentialToRequest(currentCredential + 1);
  }, [currentStep, currentCredential]);

  useEffect(() => {
    if (connectionStatus) {
      const msg = t(`credential.generic.${SUBJECT_STATUSES[connectionStatus]}`);
      const isQRStep = currentStep === QR_STEP;
      const isConnectionAccepted = connectionStatus === CONNECTION_ACCEPTED;
      // TODO change to CREDENTIAL_AVAILABLE when we can create the credential
      if (isQRStep && isConnectionAccepted) {
        setCurrentStep(currentStep + 1);
        return message.success(msg);
      }
      return message.info(msg);
      //  TODO change when we can create the credential. Actually it shows all status changes
    }
  }, [connectionStatus, currentStep, t]);

  const generateConnectionToken = async () => {
    try {
      const newToken = await getConnectionToken();
      setToken(newToken);
    } catch (error) {
      Logger.error('Error at user creation', error);
      message.error(t('credential.errors.connectionCreation'));
    }
  };

  const listenSubjectStatusChanges = () => {
    startSubjectStatusStream(token, setConnectionStatus);
  };

  const getStep = () => {
    switch (currentStep) {
      case INTRODUCTION_STEP: {
        const renderLeft = () => (
          <IntroductionInformation
            nextStep={generateConnectionToken}
            buttonDisabled={enabledCredentialToRequest !== currentCredential}
          />
        );
        const renderRight = () => <WhatYouNeed textNeed="Scan Conection QR Code" />;
        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      case QR_STEP: {
        const renderLeft = () => <ScanQRInfo />;
        const renderRight = () => <QRCard qrValue={token} />;
        return (
          <SplittedPageInside
            onMount={listenSubjectStatusChanges}
            renderLeft={renderLeft}
            renderRight={renderRight}
          />
        );
      }
      case SUCCESS_STEP: {
        const renderLeft = () => <FinishInfo />;
        const renderRight = () => <CreatedCredential />;

        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      default:
        return <div />;
    }
  };

  return (
    <Credentials
      getStep={getStep}
      changeCurrentCredential={value => setCurrentCredential(value)}
      availableCredential={currentCredential}
    />
  );
};

CredentialsContainer.propTypes = {
  api: PropTypes.shape({
    getConnectionToken: PropTypes.func,
    startSubjectStatusStream: PropTypes.func
  }).isRequired
};

export default withApi(CredentialsContainer);
