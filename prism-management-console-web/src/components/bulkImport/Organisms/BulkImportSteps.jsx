import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CompleteSpreadSheetStep from '../Molecules/Steps/CompleteSpreadSheetStep';
import AssignToGroupsStep from '../Molecules/Steps/AssignToGroupsStep';
import StepsFooter from '../../common/Molecules/StepFooter/StepFooter';
import { contactShape, credentialTypeShape } from '../../../helpers/propShapes';
import {
  COMPLETE_SPREADSHEET_STEP,
  ASSIGN_TO_GROUPS,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../../helpers/constants';
import './_style.scss';

const BulkImportSteps = ({
  fileData,
  setFileData,
  selectedGroups,
  setSelectedGroups,
  skipGroupsAssignment,
  setSkipGroupsAssignment,
  showGroupSelection,
  onFinish,
  cancelImport,
  recipients,
  credentialType,
  useCase,
  headersMapping,
  loading
}) => {
  const [currentStep, setCurrentStep] = useState(COMPLETE_SPREADSHEET_STEP);

  const { t } = useTranslation();

  const handlePreviousStep = () => setCurrentStep(currentStep - 1);

  const handleNextStep = () => setCurrentStep(currentStep + 1);

  const shouldDisableNext = {
    [COMPLETE_SPREADSHEET_STEP]: !fileData || fileData?.errors.length,
    [ASSIGN_TO_GROUPS]: !skipGroupsAssignment && !selectedGroups.length
  };

  const showStepNumber = {
    [IMPORT_CONTACTS]: true,
    [IMPORT_CREDENTIALS_DATA]: false
  };

  const isEmbedded = {
    [IMPORT_CONTACTS]: false,
    [IMPORT_CREDENTIALS_DATA]: true
  };

  const disableNext = shouldDisableNext[currentStep];

  return (
    <div className={`BulkImportStepsWrapper ${isEmbedded[useCase] ? 'EmbeddedImportWrapper' : ''}`}>
      <div className="BulkImportContent">
        <CompleteSpreadSheetStep
          currentStep={currentStep}
          setCurrentStep={setCurrentStep}
          setFileData={setFileData}
          recipients={recipients}
          credentialType={credentialType}
          showStepNumber={showStepNumber[useCase]}
          headersMapping={headersMapping}
          isEmbedded={isEmbedded[useCase]}
        />
        {showGroupSelection && (
          <AssignToGroupsStep
            currentStep={currentStep}
            setCurrentStep={setCurrentStep}
            showGroupSelection={showGroupSelection}
            selectedGroups={selectedGroups}
            setSelectedGroups={setSelectedGroups}
            setSkipGroupsAssignment={setSkipGroupsAssignment}
            disabled={shouldDisableNext[COMPLETE_SPREADSHEET_STEP]}
          />
        )}
      </div>
      <StepsFooter
        currentStep={currentStep}
        stepCount={showGroupSelection ? 2 : 1}
        onCancel={cancelImport}
        previousStep={handlePreviousStep}
        nextStep={handleNextStep}
        disableNext={disableNext}
        loading={loading}
        onFinish={onFinish}
      />
    </div>
  );
};

BulkImportSteps.defaultProps = {
  fileData: null,
  showGroupSelection: false,
  loading: false,
  selectedGroups: [],
  onFinish: () => {},
  cancelImport: () => {},
  recipients: null,
  credentialType: null
};

BulkImportSteps.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  credentialType: PropTypes.shape(credentialTypeShape),
  fileData: PropTypes.shape({
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)),
    errors: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.shape({ type: PropTypes.string })))
  }),
  setFileData: PropTypes.func.isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func.isRequired,
  showGroupSelection: PropTypes.bool,
  loading: PropTypes.bool,
  onFinish: PropTypes.func,
  cancelImport: PropTypes.func,
  skipGroupsAssignment: PropTypes.bool.isRequired,
  setSkipGroupsAssignment: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired
};

export default BulkImportSteps;
