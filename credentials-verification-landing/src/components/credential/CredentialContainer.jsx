import React, { useState, useRef, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import SplittedPage from './Organisms/SplittedPage/SplittedPage';
import Credential from './Credential';
import FinishInfo from './Molecules/FinishInfo/FinishInfo';
import CreatedCredential from './Organisms/CreatedCredential/CreatedCredential';
import { dayMonthYearFormatter, toProtoDate } from '../../helpers/formatters';
import QRCard from './Molecules/QRCard/QRCard';
import ScanQRInfo from './Molecules/ScanQRInfo/ScanQRInfo';
import PersonalInformation from './Organisms/PersonalInformation/PersonalInformation';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';

const PERSONAL_INFORMATION_STEP = 0;
const QR_STEP = 1;
const SUCCESS_STEP = 2;

const CredentialContainer = ({
  api: { createStudent, generateConnectionToken, createAndIssueCredential }
}) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(0);
  const [credentialData, setCredentialData] = useState({});
  const [student, setStudent] = useState();
  const [token, setToken] = useState('');

  const personalInfoRef = useRef();

  const generateStudent = (fullName, admissionDate) => {
    const formattedDate = toProtoDate(admissionDate);
    createStudent({
      fullName,
      admissionDate: formattedDate
    })
      .then(createdStudent => {
        setStudent(createdStudent);
        return generateConnectionToken(undefined, createdStudent.id);
      })
      .then(setToken)
      .catch(error => {
        Logger.error('Error at user creation', error);
        message.error(t('credential.errors.studentCreation'));
      });
  };

  const nextStep = () => setCurrentStep(currentStep + 1);

  useEffect(() => {
    if (student && token) {
      nextStep();
    }
  }, [student, token]);

  const submitForm = () => {
    personalInfoRef.current
      .getForm()
      .validateFieldsAndScroll(
        ['startDate', 'graduationDate', 'name', 'lastName', 'award'],
        (errors, { startDate, graduationDate, name, lastName, award }) => {
          if (errors) return;

          const fullName = `${name} ${lastName}`;

          const credentialToshow = {
            student: fullName,
            startDate: dayMonthYearFormatter(startDate),
            graduationDate: dayMonthYearFormatter(graduationDate)
          };

          setCredentialData(credentialToshow);
          generateStudent(fullName, startDate);
        }
      );
  };

  const createAndIssue = () => {
    const { startDate, graduationDate } = credentialData;

    createAndIssueCredential({
      enrollmentDate: toProtoDate(startDate),
      graduationDate: toProtoDate(graduationDate),
      studentId: student.id
    })
      .then(nextStep)
      .catch(error => {
        Logger.error('Error at credential creation and issuing', error);
        message.error('credential.errors.creationAndIssuing');

        // TODO erase this when we can test the integration with the mobile app
        if (error.message === 'User not connected') nextStep();
      });
  };

  const getStep = () => {
    switch (currentStep) {
      case PERSONAL_INFORMATION_STEP: {
        return <PersonalInformation nextStep={submitForm} personalInfoRef={personalInfoRef} />;
      }
      case QR_STEP: {
        const renderLeft = () => <ScanQRInfo nextStep={createAndIssue} />;
        const renderRight = () => <QRCard qrValue={token} />;

        return <SplittedPage renderLeft={renderLeft} renderRight={renderRight} />;
      }
      case SUCCESS_STEP: {
        const renderLeft = () => <FinishInfo />;
        const renderRight = () => <CreatedCredential credentialData={credentialData} />;

        return <SplittedPage renderLeft={renderLeft} renderRight={renderRight} />;
      }
      default:
        return <div />;
    }
  };

  return <Credential getStep={getStep} />;
};

CredentialContainer.propTypes = {
  api: PropTypes.shape({
    createStudent: PropTypes.func,
    generateConnectionToken: PropTypes.func,
    createAndIssueCredential: PropTypes.func
  }).isRequired
};

export default withApi(CredentialContainer);
