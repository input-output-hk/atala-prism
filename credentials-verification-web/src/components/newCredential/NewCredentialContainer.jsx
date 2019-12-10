import React, { useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import NewCredential from './NewCredential';
import { withApi } from '../providers/witApi';
import SaveForLaterModal from './Molecules/Modal/SaveForLaterModal';
import GroupsContainer from '../groups/GroupsContainer';
import NewCredentialValidation from './Molecules/Validation/NewCredentialValidation';
import NewCredentialCreation from './Organism/Creation/NewCredentialCreation';
import { fromUnixToProtoDateFormatter } from '../../helpers/formatters';
import { withRedirector } from '../providers/withRedirector';

const NewCredentialContainer = ({ api, redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();

  const { savePictureInS3, saveDraft, createCredential } = api;

  const [currentStep, setCurrentStep] = useState(0);
  const [degreeName, setDegreeName] = useState();
  const [award, setAward] = useState();
  const [startDate, setStartDate] = useState();
  const [graduationDate, setGraduationDate] = useState();
  const [logoUniversity, setLogoUniversity] = useState();
  const [group, setGroup] = useState();
  const [open, setOpen] = useState(false);

  const formRef = React.createRef();

  const saveCredential = () => {
    createCredential({
      title: degreeName,
      groupName: group.groupName,
      enrollmentDate: fromUnixToProtoDateFormatter(startDate),
      graduationDate: fromUnixToProtoDateFormatter(graduationDate)
    })
      .then(response => {
        Logger.info('Successfully saved the credential: ', response.toObject());
        redirectToCredentials();
      })
      .catch(error => {
        Logger.error(error);
        message.error(t('errors.errorSaving', { model: t('credentials.title') }));
      });
  };

  const createCredentialTemplate = () => {
    formRef.current
      .getForm()
      .validateFieldsAndScroll(
        ['degreeName', 'award', 'startDate', 'graduationDate', 'logoUniversity'],
        (
          errors,
          {
            degreeName: newDegreeName,
            award: newAward,
            startDate: newStartDate,
            graduationDate: newGraduationDate,
            logoUniversity: newLogoUniversity
          }
        ) => {
          if (errors) return;

          setDegreeName(newDegreeName);
          setAward(newAward);
          setStartDate(newStartDate);
          setGraduationDate(newGraduationDate);
          setLogoUniversity(newLogoUniversity[0]);
          setCurrentStep(currentStep + 1);
        }
      );
  };

  const getValuesForDraft = firstStep => {
    if (firstStep && formRef && formRef.current) {
      const form = formRef.current.getForm();

      return {
        degreeName: form.getFieldValue('degreeName'),
        award: form.getFieldValue('award'),
        startDate: form.getFieldValue('startDate'),
        graduationDate: form.getFieldValue('graduationDate'),
        logoUniversity: form.getFieldValue('logoUniversity')
      };
    }

    return {
      degreeName,
      award,
      startDate,
      graduationDate,
      logoUniversity
    };
  };

  const credentialValues = {
    award,
    degreeName,
    logoUniversity,
    startDate,
    graduationDate
  };

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return (
          <NewCredentialCreation
            credentialValues={credentialValues}
            savePicture={savePictureInS3}
            formRef={formRef}
          />
        );
      case 1: {
        return <GroupsContainer api={api} selectingProps={{ setGroup, group }} />;
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
    savePictureInS3: PropTypes.func,
    saveDraft: PropTypes.func,
    getGroups: PropTypes.func,
    createCredential: PropTypes.func
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
