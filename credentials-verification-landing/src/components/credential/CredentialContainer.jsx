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
import { CONNECTION_ACCEPTED, CONNECTION_REVOKED } from '../../helpers/constants';

const PERSONAL_INFORMATION_STEP = 0;
const QR_STEP = 1;
const SUCCESS_STEP = 2;

const CredentialContainer = ({
  api: { createStudent, getStudentById, generateConnectionToken, createAndIssueCredential }
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
    const isFirstStep = currentStep === PERSONAL_INFORMATION_STEP;

    if (isFirstStep && student && token) {
      nextStep();
    }
  }, [student, token]);

  const submitForm = () => {
    personalInfoRef.current
      .getForm()
      .validateFieldsAndScroll(
        ['startDate', 'graduationDate', 'name', 'lastName', 'award'],
        (errors, { startDate, graduationDate, name, lastName }) => {
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

  const returnAfterOneSecond = result => {
    setTimeout(result, 1000);
  };

  const checkStudentConnection = id =>
    new Promise(resolve =>
      getStudentById(id)
        .then(foundStudent => {
          if (!foundStudent) return returnAfterOneSecond(() => resolve(true));

          const { connectionstatus } = foundStudent;

          const isAccepted = connectionstatus === CONNECTION_ACCEPTED;
          if (isAccepted) createAndIssue();

          const isRevoked = connectionstatus === CONNECTION_REVOKED;
          if (isRevoked) {
            submitForm();
          }

          return returnAfterOneSecond(() => resolve(!(isAccepted || isRevoked)));
        })
        .catch(error => {
          Logger.error('Error while getting the student to check the connection status.', error);

          return returnAfterOneSecond(() => resolve(false));
        })
    );

  const loopUntilStudentIsConnected = async () => {
    let continueLooping = true;

    const { id } = student;

    do {
      try {
        // eslint-disable-next-line no-await-in-loop
        continueLooping = await checkStudentConnection(id);
      } catch (error) {
        Logger.error('An error happened while waiting for connection status change:', error);
        message.error(t('credential.error.connectionWaiting'));
        continueLooping = false;
      }
    } while (continueLooping);
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
      });
  };

  const getStep = () => {
    switch (currentStep) {
      case PERSONAL_INFORMATION_STEP: {
        return <PersonalInformation nextStep={submitForm} personalInfoRef={personalInfoRef} />;
      }
      case QR_STEP: {
        const renderLeft = () => <ScanQRInfo />;
        const renderRight = () => <QRCard qrValue={token} />;

        return (
          <SplittedPage
            onMount={loopUntilStudentIsConnected}
            renderLeft={renderLeft}
            renderRight={renderRight}
          />
        );
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
    createAndIssueCredential: PropTypes.func,
    getStudentById: PropTypes.func
  }).isRequired
};

export default withApi(CredentialContainer);
