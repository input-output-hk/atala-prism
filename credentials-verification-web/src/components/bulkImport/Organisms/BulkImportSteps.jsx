import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CompleteSpreadSheetStep from '../Molecules/Steps/CompleteSpreadSheetStep';
import AssignToGroupsStep from '../Molecules/Steps/AssignToGroupsStep';
import StepsFooter from '../../common/Molecules/StepFooter/StepFooter';
import { COMPLETE_SPREADSHEET_STEP, ASSIGN_TO_GROUPS } from '../../../helpers/constants';
import './_style.scss';

const STEP_COUNT_WITH_GROUP_SELECTION = 2;
const STEP_COUNT_WITHOUT_GROUP_SELECTION = 1;

const BulkImportSteps = ({
  fileData,
  setFileData,
  selectedGroups,
  setSelectedGroups,
  skipGroupsAssignment,
  setSkipGroupsAssignment,
  showGroupSelection,
  onFinish,
  cancelImport
}) => {
  const [currentStep, setCurrentStep] = useState(COMPLETE_SPREADSHEET_STEP);

  const { t } = useTranslation();

  const handlePreviousStep = () => setCurrentStep(currentStep - 1);

  const handleNextStep = () => setCurrentStep(currentStep + 1);

  const shouldDisableNext = {
    [COMPLETE_SPREADSHEET_STEP]: !fileData || fileData?.errors.length,
    [ASSIGN_TO_GROUPS]: !skipGroupsAssignment && !selectedGroups.length
  };

  const disableNext = shouldDisableNext[currentStep];

  const stepCount = showGroupSelection
    ? STEP_COUNT_WITH_GROUP_SELECTION
    : STEP_COUNT_WITHOUT_GROUP_SELECTION;

  return (
    <>
      <div className="ContentHeader">
        <h1>{t('bulkImport.title')}</h1>
        <p>{t('bulkImport.info')}</p>
      </div>
      <div className="BulkImportContent">
        <CompleteSpreadSheetStep
          currentStep={currentStep}
          setCurrentStep={setCurrentStep}
          setFileData={setFileData}
          // TODO: implement passing existing contacts and a credential type
          // inputData={{contacts: [...], credentialType: {...}}}
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
        stepCount={stepCount}
        onCancel={cancelImport}
        previousStep={handlePreviousStep}
        nextStep={handleNextStep}
        disableNext={disableNext}
        onFinish={onFinish}
      />
    </>
  );
};

BulkImportSteps.defaultProps = {
  fileData: null,
  showGroupSelection: false,
  selectedGroups: [],
  onFinish: () => {},
  cancelImport: () => {},
  skipGroupsAssignment: false,
  setSkipGroupsAssignment: null
};

BulkImportSteps.propTypes = {
  fileData: PropTypes.shape({
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)),
    errors: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.shape({ type: PropTypes.string })))
  }),
  setFileData: PropTypes.func.isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func.isRequired,
  showGroupSelection: PropTypes.bool,
  onFinish: PropTypes.func,
  cancelImport: PropTypes.func,
  skipGroupsAssignment: PropTypes.bool,
  setSkipGroupsAssignment: PropTypes.func
};

export default BulkImportSteps;
