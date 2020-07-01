import React, { useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import NewCredential from './NewCredential';
import { withApi } from '../providers/withApi';
import SaveForLaterModal from './Molecules/Modal/SaveForLaterModal';
import GroupsContainer from '../groups/GroupsContainer';
import NewCredentialValidation from './Molecules/Validation/NewCredentialValidation';
import NewCredentialCreation from './Organism/Creation/NewCredentialCreation';
import { fromUnixToProtoDateFormatter } from '../../helpers/formatters';
import { withRedirector } from '../providers/withRedirector';
import { EXAMPLE_UNIVERSITY_NANE } from '../../helpers/constants';
import { imageToFileReader } from '../../helpers/fileHelpers';

const NewCredentialContainer = ({ api, redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();

  const { saveDraft } = api;

  const [currentStep, setCurrentStep] = useState(0);
  const [degreeName, setDegreeName] = useState();
  const [startDate, setStartDate] = useState();
  const [graduationDate, setGraduationDate] = useState();
  const [group, setGroup] = useState();
  const [open, setOpen] = useState(false);

  const formRef = React.createRef();

  const saveCredential = async () => {
    const students = await api.subjectsManager.getAllSubjects(group.name);
    api.credentialsManager
      .createCredential({
        title: degreeName,
        groupName: group.name,
        enrollmentdate: fromUnixToProtoDateFormatter(startDate),
        graduationdate: fromUnixToProtoDateFormatter(graduationDate),
        students
      })
      .then(() => {
        Logger.info('Successfully saved the credential');
        redirectToCredentials();
      })
      .catch(error => {
        Logger.error(error);
        message.error(t('errors.errorSaving', { model: t('credentials.title') }));
      });
  };

  const updateExampleCredential = (key, value) => {
    const updaterDictionary = {
      degreeName: setDegreeName,
      startDate: setStartDate,
      graduationDate: setGraduationDate
    };

    updaterDictionary[key](value);
  };

  const createCredentialTemplate = () => {
    formRef.current
      .getForm()
      .validateFieldsAndScroll(
        ['degreeName', 'startDate', 'graduationDate'],
        (
          errors,
          { degreeName: newDegreeName, startDate: newStartDate, graduationDate: newGraduationDate }
        ) => {
          if (errors) return;

          setDegreeName(newDegreeName);
          setStartDate(newStartDate);
          setGraduationDate(newGraduationDate);
          setCurrentStep(currentStep + 1);
        }
      );
  };

  const getValuesForDraft = firstStep => {
    if (firstStep && formRef && formRef.current) {
      const form = formRef.current.getForm();

      return {
        degreeName: form.getFieldValue('degreeName'),
        startDate: form.getFieldValue('startDate'),
        graduationDate: form.getFieldValue('graduationDate')
      };
    }

    return {
      degreeName,
      startDate,
      graduationDate
    };
  };

  const credentialValues = {
    degreeName,
    startDate,
    graduationDate
  };

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return (
          <NewCredentialCreation
            updateExampleCredential={updateExampleCredential}
            credentialValues={credentialValues}
            savePicture={imageToFileReader}
            formRef={formRef}
            credentialData={{
              title: degreeName,
              university: EXAMPLE_UNIVERSITY_NANE,
              startDate,
              graduationDate
            }}
          />
        );
      case 1: {
        return (
          <div className="GroupsNewCredential">
            <GroupsContainer api={api} selectingProps={{ setGroup, group }} />
          </div>
        );
      }
      default:
        return <NewCredentialValidation credentialValues={credentialValues} group={group} />;
    }
  };

  const renderModal = () => {
    const closeModal = () => setOpen(false);

    return (
      <SaveForLaterModal
        open={open}
        onCancel={closeModal}
        onOk={() =>
          saveDraft(getValuesForDraft(currentStep === 0))
            .then(() => {
              message.success(t('newCredential.modal.saveSuccess'));
              closeModal();
            })
            .catch(({ message: errorMessage }) => {
              Logger.error('Error while saving draft', t(errorMessage));
              message.error(t(errorMessage));
              closeModal();
            })
        }
      />
    );
  };

  return (
    <NewCredential
      currentStep={currentStep}
      createCredentialTemplate={createCredentialTemplate}
      changeStep={setCurrentStep}
      saveCredential={saveCredential}
      renderModal={renderModal}
      openModal={() => setOpen(true)}
      renderStep={renderStep}
      selectedGroup={group}
    />
  );
};

NewCredentialContainer.propTypes = {
  api: PropTypes.shape({
    saveDraft: PropTypes.func,
    getGroups: PropTypes.func,
    credentialsManager: PropTypes.shape({ createCredential: PropTypes.func }).isRequired,
    subjectsManager: PropTypes.shape({ getAllSubjects: PropTypes.func }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
