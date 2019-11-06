import React, { useState, createRef, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import _ from 'lodash';
import DocumentAcceptation from './Molecules/DocumentAcceptation/DocumentAcceptation';
import Registration from './Registration';
import { withApi } from '../providers/witApi';
import DownloadWallet from './Molecules/DownloadWallet/DownloadWallet';
import SeedPhrase from './Organisms/SeedPhrase/SeedPhrase';
import VerifySeedPhrase from './Organisms/SeedVerificator/VerifySeedPhrase';
import PasswordSetup from './Organisms/PasswordSetup/PasswordSetup';
import OrganisationInfo from './Organisms/OrganisationInfo/OrganisationInfo';
import Congratulations from './Atoms/Congratulations/Congratulations';

const TERMS_AND_CONDITIONS_STEP = 0;
const PRIVACY_POLICY_STEP = 1;
const DOWNLOAD_WALLET_STEP = 2;
const PASSWORD_STEP = 3;
const SEED_PHRASE_STEP = 4;
const MNEMONIC_VALIDATION_STEP = 5;
const ORGANISATION_INFO_STEP = 6;
const STEP_QUANTITY = 7;

const RegistrationContainer = ({ api: { getTermsAndConditions, getPrivacyPolicy } }) => {
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
  const [, setOrganisationInfo] = useState('');

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
  const organisationRef = createRef();

  const nextStep = () => setCurrentStep(currentStep + 1);

  const nextFromDocument = documentType => {
    if (!accepted)
      return message.error(
        t('registration.acceptToContinue', { document: t(`registration.${documentType}`) })
      );

    setAccepted(currentStep === SEED_PHRASE_STEP);
    nextStep();
  };

  const validateWalletStatus = () =>
    new Promise((resolve, reject) => {
      const randomNum = 100 * Math.random();

      if (randomNum > 70) reject();
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

  const nextIfMnemonicIsValid = () =>
    validMnemonic
      ? nextStep()
      : message.error(t('registration.mnemonic.validation.validationRequired'));

  const validatePassword = () =>
    passwordRef.current.getForm().validateFieldsAndScroll((errors, { passwordConfirmation }) => {
      if (errors) return;

      setPassword(passwordConfirmation);
      nextStep();
    });

  const validateOrganisatonInfo = () =>
    organisationRef.current.getForm().validateFieldsAndScroll((errors, { organisationInfo }) => {
      if (errors) return;

      setOrganisationInfo(organisationInfo);
      nextStep();
    });

  const nextFunction = () => {
    switch (currentStep) {
      case TERMS_AND_CONDITIONS_STEP:
        return () => nextFromDocument(t('termsAndConditions'));
      case PRIVACY_POLICY_STEP:
        return () => nextFromDocument(t('privacyPolicy'));
      case DOWNLOAD_WALLET_STEP:
        return nextIfWalletIsRunning;
      case PASSWORD_STEP:
        return validatePassword;
      case SEED_PHRASE_STEP:
        return () => nextFromDocument(t('seedPhrase'));
      case MNEMONIC_VALIDATION_STEP:
        return nextIfMnemonicIsValid;
      case ORGANISATION_INFO_STEP:
        return validateOrganisatonInfo;
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
            lastUpdated={9}
            content={termsAndConditions}
          />
        );
      case PRIVACY_POLICY_STEP:
        return (
          <DocumentAcceptation
            title="registration.privacyPolicy"
            lastUpdated={9}
            content={privacyPolicy}
          />
        );
      case DOWNLOAD_WALLET_STEP:
        return <DownloadWallet walletError={walletError} />;
      case PASSWORD_STEP:
        return <PasswordSetup password={password} passwordRef={passwordRef} />;
      case SEED_PHRASE_STEP:
        return (
          <SeedPhrase
            accepted={accepted}
            toggleAccept={() => setAccepted(!accepted)}
            mnemonics={mnemonics}
          />
        );
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
      case ORGANISATION_INFO_STEP:
        return <OrganisationInfo organisationRef={organisationRef} />;
      default:
        return <Congratulations />;
    }
  };

  return (
    <Registration
      renderContent={() => getContent(currentStep)}
      next={nextFunction()}
      accepted={accepted}
      toggleAccept={currentStep < DOWNLOAD_WALLET_STEP ? () => setAccepted(!accepted) : null}
      previous={
        currentStep === MNEMONIC_VALIDATION_STEP ? () => setCurrentStep(SEED_PHRASE_STEP) : null
      }
      renderFooter={currentStep < STEP_QUANTITY}
      documentToAccept={currentStep ? 'privacyPolicy' : 'termsAndConditions'}
    />
  );
};

RegistrationContainer.propTypes = {
  api: PropTypes.shape({ getTermsAndConditions: PropTypes.func, getPrivacyPolicy: PropTypes.func })
    .isRequired
};

export default withApi(RegistrationContainer);
