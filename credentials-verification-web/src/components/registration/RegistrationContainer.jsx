import React, { useState, createRef, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import _ from 'lodash';
import moment from 'moment';
import DocumentAcceptation from './Molecules/DocumentAcceptation/DocumentAcceptation';
import Registration from './Registration';
import { withApi } from '../providers/withApi';
import DownloadWallet from './Molecules/DownloadWallet/DownloadWallet';
import SeedPhrase from './Organisms/SeedPhrase/SeedPhrase';
import VerifySeedPhrase from './Organisms/SeedVerificator/VerifySeedPhrase';
import PasswordSetup from './Organisms/PasswordSetup/PasswordSetup';
import OrganizationInfo from './Organisms/OrganizationInfo/OrganizationInfo';
import Congratulations from './Atoms/Congratulations/Congratulations';
import Logger from '../../helpers/Logger';
import { imageToFileReader } from '../../helpers/fileHelpers';

const TERMS_AND_CONDITIONS_STEP = 0;
const PRIVACY_POLICY_STEP = 1;
const DOWNLOAD_WALLET_STEP = 2;
const PASSWORD_STEP = 3;
const SEED_PHRASE_STEP = 4;
const MNEMONIC_VALIDATION_STEP = 5;
const ORGANIZATION_INFO_STEP = 6;
const STEP_QUANTITY = 7;

const RegistrationContainer = ({
  api: { getTermsAndConditions, getPrivacyPolicy, createWallet, lockWallet, registerUser }
}) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(TERMS_AND_CONDITIONS_STEP);
  const [accepted, setAccepted] = useState(false);
  const [termsAndConditions, setTermsAndConditions] = useState('');
  const [privacyPolicy, setPrivacyPolicy] = useState('');
  const [walletError, setWalletError] = useState(false);
  const [mnemonics, setMnemonics] = useState([]);
  const [validMnemonic, setValidMnemonic] = useState(false);
  const [mnemonicWords, setMnemonicWords] = useState([]);
  const [password, setPassword] = useState('');
  const [organizationInfo, setOrganizationInfo] = useState({});

  useEffect(() => {
    const mnemonicsFromWallet = [
      'fan',
      'enter',
      'win',
      'brick',
      'sniff',
      'act',
      'doll',
      'until',
      'test',
      'comic',
      'deposit',
      'bicycle'
    ];

    const transformedMnemonics = mnemonicsFromWallet.map((value, index) => ({ value, index }));
    setMnemonics(transformedMnemonics);
  }, []);

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

  const passwordRef = createRef();
  const organizationRef = createRef();

  const nextStep = () => setCurrentStep(currentStep + 1);

  const validateWalletStatus = () =>
    new Promise(resolve => {
      resolve();
    });

  const nextIfWalletIsRunning = () => {
    validateWalletStatus()
      .then(() => {
        setWalletError(false);
        nextStep();
      })
      .catch(() => setWalletError(true));
  };

  const nextIfAcceptedSeedPhrase = () =>
    accepted ? nextStep() : message.error(t('registration.acceptSeedPhraseToContinue'));

  const isDisabled = () => currentStep === MNEMONIC_VALIDATION_STEP && !validMnemonic;

  const validatePassword = () =>
    passwordRef.current.getForm().validateFieldsAndScroll((errors, { passwordConfirmation }) => {
      if (errors) return;

      setPassword(passwordConfirmation);
      nextStep();
    });

  const validateOrganizatonInfo = async () =>
    organizationRef.current
      .getForm()
      .validateFieldsAndScroll((errors, { organizationName, organizationRole, logo }) => {
        if (errors) return;
        setOrganizationInfo({ organizationName, organizationRole, logo });

        createWallet(password, organizationName, organizationRole, logo[0])
          .then(did => {
            registerUser(organizationName, did, logo);
          })
          .then(lockWallet)
          .then(nextStep)
          .catch(error => {
            Logger.error('Error at registration: ', error);
            message.error(t('errors.errorDuringRegister'));
          });
      });

  const nextFunction = () => {
    switch (currentStep) {
      case TERMS_AND_CONDITIONS_STEP:
      case PRIVACY_POLICY_STEP:
        return nextStep;
      case DOWNLOAD_WALLET_STEP:
        return nextIfWalletIsRunning;
      case PASSWORD_STEP:
        return validatePassword;
      case SEED_PHRASE_STEP:
        return nextIfAcceptedSeedPhrase;
      case MNEMONIC_VALIDATION_STEP:
        return nextStep;
      case ORGANIZATION_INFO_STEP:
        return validateOrganizatonInfo;
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
      case PASSWORD_STEP:
        return <PasswordSetup password={password} passwordRef={passwordRef} />;
      case SEED_PHRASE_STEP: {
        return (
          <SeedPhrase
            accepted={accepted}
            toggleAccept={() => setAccepted(!accepted)}
            mnemonics={mnemonics}
          />
        );
      }
      case MNEMONIC_VALIDATION_STEP: {
        if (!mnemonicWords.length) {
          const WORD_QUANTITY = 2;
          const [firstWord, secondWord] = _.sampleSize(mnemonics, WORD_QUANTITY);

          const words =
            firstWord.index < secondWord.index ? [firstWord, secondWord] : [secondWord, firstWord];

          setMnemonicWords(words);
        }

        return (
          <VerifySeedPhrase
            validMnemonic={validMnemonic}
            setValidMnemonic={setValidMnemonic}
            words={mnemonicWords}
          />
        );
      }
      case ORGANIZATION_INFO_STEP:
        return (
          <OrganizationInfo
            organizationRef={organizationRef}
            organizationInfo={organizationInfo}
            savePicture={imageToFileReader}
          />
        );
      default:
        return <Congratulations />;
    }
  };

  const requiresAgreement = () => {
    const stepsWithAgreement = [TERMS_AND_CONDITIONS_STEP, PRIVACY_POLICY_STEP, SEED_PHRASE_STEP];

    return stepsWithAgreement.includes(currentStep);
  };

  const footerProps = {
    next: nextFunction(),
    previous: currentStep ? () => setCurrentStep(currentStep - 1) : null,
    requiresAgreement: requiresAgreement(),
    disabled: isDisabled()
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
    createWallet: PropTypes.func.isRequired,
    lockWallet: PropTypes.func.isRequired,
    registerUser: PropTypes.func.isRequired
  }).isRequired
};

export default withApi(RegistrationContainer);
