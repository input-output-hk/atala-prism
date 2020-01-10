import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import GenericStep from './GenericStep';
import { UPLOAD_STEP } from '../../../../helpers/constants';
import FileUploader from '../../../common/Molecules/FileUploader/FileUploader';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { refShape } from '../../../../helpers/propShapes';

const UploadStep = ({ currentStep, uploadRef, uploadBulkExcel, setAsCurrentStep }) => {
  const { t } = useTranslation();

  const uploaderProps = {
    field: 'usersBulk',
    savePicture: uploadBulkExcel,
    uploadText: t('bulkImport.upload.buttonText'),
    formRef: uploadRef,
    disabled: currentStep !== UPLOAD_STEP
  };

  const items = [
    {
      key: 'memes',
      input: <FileUploader {...uploaderProps} />
    }
  ];

  const props = {
    step: UPLOAD_STEP,
    currentStep,
    stepType: 'upload',
    button: <CustomForm items={items} ref={uploadRef} />,
    changeStep: setAsCurrentStep
  };

  return <GenericStep {...props} />;
};

UploadStep.propTypes = {
  currentStep: PropTypes.number.isRequired,
  uploadRef: refShape.isRequired,
  uploadBulkExcel: PropTypes.func.isRequired,
  setAsCurrentStep: PropTypes.func.isRequired
};

export default UploadStep;
