import React, { useState } from 'react';
import PropTypes from 'prop-types';
import CompleteSpreadSheetStep from '../Molecules/Steps/CompleteSpreadSheetStep';
import AssignToGroupsStep from '../Molecules/Steps/AssignToGroupsStep';
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
  selectedGroupIds,
  setSelectedGroupIds,
  skipGroupsAssignment,
  setSkipGroupsAssignment,
  showGroupSelection,
  recipients,
  credentialType,
  useCase,
  headersMapping
}) => {
  const [currentStep, setCurrentStep] = useState(COMPLETE_SPREADSHEET_STEP);

  const shouldDisableNext = {
    [COMPLETE_SPREADSHEET_STEP]: !fileData || fileData?.errors.length,
    [ASSIGN_TO_GROUPS]: !skipGroupsAssignment && !selectedGroupIds.length
  };

  const showStepNumber = {
    [IMPORT_CONTACTS]: true,
    [IMPORT_CREDENTIALS_DATA]: false
  };

  const isEmbedded = {
    [IMPORT_CONTACTS]: false,
    [IMPORT_CREDENTIALS_DATA]: true
  };

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
            selectedGroupIds={selectedGroupIds}
            setSelectedGroupIds={setSelectedGroupIds}
            setSkipGroupsAssignment={setSkipGroupsAssignment}
            disabled={shouldDisableNext[COMPLETE_SPREADSHEET_STEP]}
          />
        )}
      </div>
    </div>
  );
};

BulkImportSteps.defaultProps = {
  fileData: null,
  showGroupSelection: false,
  selectedGroupIds: [],
  recipients: null,
  credentialType: null
};

BulkImportSteps.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  credentialType: credentialTypeShape,
  fileData: PropTypes.shape({
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)),
    errors: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.shape({ type: PropTypes.string })))
  }),
  setFileData: PropTypes.func.isRequired,
  selectedGroupIds: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroupIds: PropTypes.func.isRequired,
  showGroupSelection: PropTypes.bool,
  skipGroupsAssignment: PropTypes.bool.isRequired,
  setSkipGroupsAssignment: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired
};

export default BulkImportSteps;
