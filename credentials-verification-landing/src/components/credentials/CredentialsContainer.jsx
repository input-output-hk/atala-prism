import React, { useState, useEffect, useContext } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import FinishInfo from './Molecules/FinishInfo/FinishInfo';
import CongratsStep from './Molecules/CongratsStep/CongratsStep';
import CreatedCredential from './Organisms/CreatedCredential/CreatedCredential';
import QRCard from './Molecules/QRCard/QRCard';
import ScanQRInfo from './Molecules/ScanQRInfo/ScanQRInfo';
import IntroductionInformation from './Organisms/IntroductionInformation/IntroductionInformation';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { toProtoDate } from '../../helpers/formatters';
import SendCredentials from './Molecules/SendCredential/SendCredentials';
import RequestedCredentials from './Molecules/RequestedCredentials/RequestedCredentials';
import {
  CREDENTIAL_TYPES,
  CREDENTIAL_SENT,
  SUBJECT_STATUSES,
  CONNECTED,
  GOVERNMENT_ISSUED_DIGITAL_IDENTITY
} from '../../helpers/constants';
import Credentials from './Credentials';
import SplittedPageInside from './Organisms/SplittedPageInside/SplittedPageInside';
import WhatYouNeed from './Molecules/WhatYouNeed/WhatYouNeed';
import { UserContext } from '../providers/userContext';
import { withRedirector } from '../providers/withRedirector';
import InteractiveMap from '../common/Organisms/InteractiveMap/InteractiveMap';

// Credentials steps
const INTRODUCTION_STEP = 0;
const QR_STEP = 1;
const SEND_CREDENTIAL_STEP = 2;
const SUCCESS_STEP = 3;

const CREDENTIALS = {
  0: 'citizenId',
  1: 'studentId',
  2: 'employeeId',
  3: 'insuredId'
};

const lastCredential = Object.keys(CREDENTIAL_TYPES).length - 1;

const CredentialsContainer = ({
  redirector: { redirectToUserInfo },
  api: { getConnectionToken, startSubjectStatusStream, setPersonalData }
}) => {
  const { t } = useTranslation();

  const { user, setUser } = useContext(UserContext);
  const [currentStep, setCurrentStep] = useState(0);
  const [token, setToken] = useState('');
  const [enabledCredentialToRequest, setEnabledCredentialToRequest] = useState(
    GOVERNMENT_ISSUED_DIGITAL_IDENTITY
  );
  const [currentCredential, setCurrentCredential] = useState(GOVERNMENT_ISSUED_DIGITAL_IDENTITY);
  const [connectionStatus, setConnectionStatus] = useState();
  const [showContactButton, setShowContactButton] = useState(false);
  const [showCongrats, setShowCongrats] = useState(false);
  const [mapInitFirstStep, setMapInitFirstStep] = useState(false);

  useEffect(() => {
    if (_.isEmpty(user)) redirectToUserInfo();
  }, []);

  useEffect(() => {
    const { citizenId, studentId, employeeId, insuredId } = user;

    const credentialsIdsArray = [citizenId, studentId, employeeId, insuredId];
    const nextCredential = credentialsIdsArray.findIndex(element => !element);
    if (nextCredential > GOVERNMENT_ISSUED_DIGITAL_IDENTITY) {
      setCurrentCredential(nextCredential);
      setEnabledCredentialToRequest(nextCredential);
    }
    if (nextCredential === -1) {
      setCurrentCredential(nextCredential);
      setShowCongrats(true);
    }
  }, []);

  useEffect(() => {
    cleanCredentialState();
  }, [currentCredential]);

  useEffect(() => {
    const isIntroductionStep = currentStep === INTRODUCTION_STEP;
    const isIdCredential = currentCredential === GOVERNMENT_ISSUED_DIGITAL_IDENTITY;
    if (token && isIntroductionStep) initQRStep();
    if (token && isIntroductionStep && isIdCredential) setMapInitFirstStep(true);
  }, [token, currentStep]);

  useEffect(() => {
    const isSuccessStep = currentStep === SUCCESS_STEP;
    if (isSuccessStep) {
      setUser({ [CREDENTIALS[currentCredential]]: token });
      setEnabledCredentialToRequest(currentCredential + 1);
    }
  }, [currentStep]);

  useEffect(() => {
    const isQRStep = currentStep === QR_STEP;
    const isSendCredentialStep = currentStep === SEND_CREDENTIAL_STEP;
    const isConnected = connectionStatus === CONNECTED;
    const isCredentialSended = connectionStatus === CREDENTIAL_SENT;
    const notIdCredential = currentCredential !== GOVERNMENT_ISSUED_DIGITAL_IDENTITY;
    if (isQRStep && isConnected && notIdCredential) {
      setCurrentStep(currentStep + 1);
      const msg = t(`credential.generic.${SUBJECT_STATUSES[connectionStatus]}`);
      return message.success(msg);
    }
    if (isQRStep && isCredentialSended) {
      setCurrentStep(currentStep + 2);
      const msg = t(`credential.generic.${SUBJECT_STATUSES[connectionStatus]}`);
      return message.success(msg);
    }
    if (isSendCredentialStep && isCredentialSended) {
      setCurrentStep(currentStep + 1);
      const msg = t(`credential.generic.${SUBJECT_STATUSES[connectionStatus]}`);
      return message.success(msg);
    }
  }, [connectionStatus]);

  useEffect(() => {
    const isLastCredential = lastCredential === currentCredential;
    const isSuccessStep = currentStep === SUCCESS_STEP;
    if (isLastCredential && isSuccessStep) setShowContactButton(true);
  }, [currentStep]);

  const generateConnectionToken = async () => {
    try {
      const newToken = await getConnectionToken(currentCredential);
      setToken(newToken);
    } catch (error) {
      Logger.error('Error at user creation', error);
      message.error(t('credential.errors.connectionCreation'));
    }
  };

  const initQRStep = () => {
    setCurrentStep(currentStep + 1);
    setPersonalData({
      connectionToken: token,
      firstName: user.firstName,
      dateOfBirth: toProtoDate(user.dateOfBirth)
    });
    listenSubjectStatusChanges();
  };

  const cleanCredentialState = () => {
    setToken('');
    setCurrentStep(INTRODUCTION_STEP);
    setConnectionStatus(undefined);
    setShowContactButton(false);
  };

  const handleStreamError = () => {
    message.error(t('credential.errors.listenSubjectStatusChanges'));
    cleanCredentialState();
  };

  const listenSubjectStatusChanges = () => {
    startSubjectStatusStream(currentCredential, token, setConnectionStatus, handleStreamError);
  };

  const jumpToNextCredential = () => {
    setCurrentCredential(currentCredential + 1);
  };

  const getStep = () => {
    switch (currentStep) {
      case INTRODUCTION_STEP: {
        const renderLeft = () => (
          <IntroductionInformation
            nextStep={generateConnectionToken}
            buttonDisabled={enabledCredentialToRequest !== currentCredential}
            currentCredential={currentCredential}
          />
        );
        const renderRight = () => <WhatYouNeed currentCredential={currentCredential} />;
        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      case QR_STEP: {
        const renderLeft = () => <ScanQRInfo currentCredential={currentCredential} />;
        const renderRight = () => (
          <QRCard
            qrValue={token}
            showDownloadHelp={currentCredential === GOVERNMENT_ISSUED_DIGITAL_IDENTITY}
          />
        );
        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      case SEND_CREDENTIAL_STEP: {
        const renderLeft = () => <SendCredentials currentCredential={currentCredential} />;
        const renderRight = () => <RequestedCredentials currentCredential={currentCredential} />;
        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      case SUCCESS_STEP: {
        const renderLeft = () => (
          <FinishInfo
            confirmSuccessCredential={jumpToNextCredential}
            currentCredential={currentCredential}
          />
        );
        const renderRight = () => <CreatedCredential currentCredential={currentCredential} />;

        return <SplittedPageInside renderLeft={renderLeft} renderRight={renderRight} />;
      }
      default:
        return <div />;
    }
  };

  return (
    <div className="InteractiveDemoContainer">
      <InteractiveMap mapStep={currentCredential + 1} mapInitFirstStep />
      <Credentials
        getStep={showCongrats ? () => <CongratsStep /> : getStep}
        changeCurrentCredential={value => setCurrentCredential(value)}
        availableCredential={currentCredential}
        showContactButton={showContactButton}
      />
    </div>
  );
};

CredentialsContainer.propTypes = {
  api: PropTypes.shape({
    getConnectionToken: PropTypes.func,
    startSubjectStatusStream: PropTypes.func,
    setPersonalData: PropTypes.func
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToUserInfo: PropTypes.func
  }).isRequired
};

export default withRedirector(withApi(CredentialsContainer));
