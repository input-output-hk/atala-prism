import React, { useContext, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { arrayOfArraysToObjects } from '../../helpers/fileHelpers';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';
import ImportTypeSelectionContainer from '../ImportTypeSelection/ImportTypeSelectionContainer';
import ManualImportContainer from '../manualImport/ManualImportContainer';
import { ImportResults } from './Molecules/ImportResults';
import { translateBackSpreadsheetNamesToContactKeys } from '../../helpers/contactValidations';
import {
  BULK_IMPORT,
  MANUAL_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  IMPORT_CREDENTIAL_DATA_STEP,
  CREDENTIAL_TYPE_FIELD_TYPES,
  DEFAULT_DATE_FORMAT
} from '../../helpers/constants';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import { createBlankContact } from '../../helpers/importHelpers';
import { DynamicFormContext } from '../providers/DynamicFormProvider';
import Logger from '../../helpers/Logger';
import { getFirstError } from '../../helpers/formHelpers';
import BulkImportSteps from '../bulkImport/Organisms/BulkImportSteps';
import './_style.scss';

const showGroupSelection = {
  [IMPORT_CONTACTS]: true,
  [IMPORT_CREDENTIALS_DATA]: false
};

const isEmbedded = {
  [IMPORT_CONTACTS]: false,
  [IMPORT_CREDENTIALS_DATA]: true
};

const RESULTS_STEP = 2;
const IMPORT_STEP = 1;
const SELECTION_STEP = 0;

const ImportDataContainer = ({
  bulkValidator,
  onFinish,
  onCancel,
  useCase,
  recipients,
  credentialType,
  headersMapping,
  loading,
  hasSelectedRecipients,
  continueCallback
}) => {
  const [currentStep, setCurrentStep] = useState(SELECTION_STEP);
  const [selectedMethod, setSelectedMethod] = useState();
  const [results, setResults] = useState();

  const [contacts, setContacts] = useState([createBlankContact(0)]);
  const [fileData, setFileData] = useState();
  const [skipGroupsAssignment, setSkipGroupsAssignment] = useState(false);
  const [selectedGroupIds, setSelectedGroupIds] = useState([]);

  const { saveFormProviderAvailable, addEntity, form } = useContext(DynamicFormContext);

  const { t } = useTranslation();

  const resetSelection = () => setSelectedMethod();

  const onSuccess = res => {
    setResults(res);
    next();
  };

  const handleManualImport = payload => onFinish(payload, onSuccess);

  const handleBulkImport = () => {
    const { dataObjects, containsErrors, validationErrors } = parseFile(fileData, bulkValidator);
    if (containsErrors) onSuccess({ fileData, validationErrors });
    else {
      const translatedContacts = translateBackSpreadsheetNamesToContactKeys(
        dataObjects,
        headersMapping
      );

      const payload = {
        [IMPORT_CONTACTS]: {
          contacts: translatedContacts,
          groups: skipGroupsAssignment ? [] : selectedGroupIds
        },
        [IMPORT_CREDENTIALS_DATA]: {
          credentials: translatedContacts
        }
      };

      onFinish(payload[useCase], onSuccess);
    }
  };

  const parseFile = (file, validator) => {
    const inputHeaders = file.data[0];
    const dataObjects = arrayOfArraysToObjects(file.data);

    const dataObjectsKey = {
      [IMPORT_CONTACTS]: 'newContacts',
      [IMPORT_CREDENTIALS_DATA]: 'newCredentials'
    };

    const { containsErrors, validationErrors } = validator({
      [dataObjectsKey[useCase]]: dataObjects,
      inputHeaders,
      headersMapping
    });

    return {
      dataObjects,
      containsErrors,
      validationErrors
    };
  };

  const handleSaveContacts = async () => {
    try {
      const data = form.getFieldValue(IMPORT_CONTACTS);
      const parsedData = data.map((item, key) => ({ ...item, key }));
      await form.validateFields();
      handleManualImport({ contacts: parsedData, groups: selectedGroupIds });
    } catch (error) {
      Logger.error('An error occurred while saving contacts', error);
      message.error(getFirstError(error));
    }
  };

  const handleSaveCredentials = () => {
    const data = form.getFieldValue(IMPORT_CREDENTIALS_DATA);
    const dateFieldKeys = credentialType.fields.filter(
      ({ type }) => type === CREDENTIAL_TYPE_FIELD_TYPES.DATE
    );

    const credentialsWithParsedDates = data.map(cred =>
      dateFieldKeys.reduce(
        (acc, { key }) => Object.assign(acc, { [key]: cred[key].format(DEFAULT_DATE_FORMAT) }),
        cred
      )
    );

    handleManualImport({ credentials: credentialsWithParsedDates });
  };

  const handleSave = () => {
    if (selectedMethod === BULK_IMPORT) handleBulkImport();
    else if (useCase === IMPORT_CONTACTS) handleSaveContacts();
    else handleSaveCredentials();
  };

  const useCaseProps = {
    useCase,
    showGroupSelection: showGroupSelection[useCase],
    isEmbedded: isEmbedded[useCase],
    continueCallback: continueCallback || results?.continueCallback
  };

  const isResultsStep = currentStep === RESULTS_STEP;
  const hasErrors = results?.validationErrors?.length;

  const renderStep = () => {
    if (isResultsStep)
      return (
        <ImportResults results={results} importType={selectedMethod} useCaseProps={useCaseProps} />
      );

    if (currentStep === IMPORT_STEP)
      return selectedMethod === BULK_IMPORT ? (
        <BulkImportSteps
          cancelImport={resetSelection}
          recipients={recipients}
          credentialType={credentialType}
          headersMapping={headersMapping}
          loading={loading}
          fileData={fileData}
          setFileData={setFileData}
          selectedGroupIds={selectedGroupIds}
          setSelectedGroupIds={setSelectedGroupIds}
          skipGroupsAssignment={skipGroupsAssignment}
          setSkipGroupsAssignment={setSkipGroupsAssignment}
          {...useCaseProps}
        />
      ) : (
        <ManualImportContainer
          addEntity={addEntity}
          useCaseProps={useCaseProps}
          credentialType={credentialType}
          hasSelectedRecipients={hasSelectedRecipients}
          contacts={contacts}
          setContacts={setContacts}
          recipients={recipients}
          selectedGroupIds={selectedGroupIds}
          setSelectedGroupIds={setSelectedGroupIds}
        />
      );

    return (
      <ImportTypeSelectionContainer
        selectedMethod={selectedMethod}
        setSelectedMethod={setSelectedMethod}
        isEmbedded={isEmbedded[useCase]}
        useCase={useCase}
        selectedRecipientsAmount={recipients?.length}
      />
    );
  };

  const isImportStep = currentStep === IMPORT_STEP;
  const isManualImport = selectedMethod === MANUAL_IMPORT;
  const shouldDisableImport = isManualImport
    ? !saveFormProviderAvailable
    : !fileData ||
      fileData.errors.length ||
      (useCase === IMPORT_CONTACTS && !skipGroupsAssignment && !selectedGroupIds.length);

  const disableNext = loading || !selectedMethod || (isImportStep && shouldDisableImport);

  const back = () => setCurrentStep(s => s - 1);
  const next = () => setCurrentStep(s => s + 1);

  const getSteps = () => {
    const baseSteps = [
      { back: onCancel, next },
      { back, next: handleSave },
      hasErrors ? { back } : { next: continueCallback || results?.continueCallback }
    ];
    if (!isEmbedded[useCase]) return baseSteps;
    return [{}, {}, baseSteps[currentStep], {}, {}];
  };

  const getTranslationSuffix = () => {
    if (isImportStep) return `.${selectedMethod}`;
    if (isResultsStep) return hasErrors ? '.errorLog' : '.success';
    return '';
  };

  const title = {
    [IMPORT_CONTACTS]: t(`importContacts.title.step${currentStep + 1}${getTranslationSuffix()}`),
    [IMPORT_CREDENTIALS_DATA]: t(
      `newCredential.title.embeddedStep${currentStep + 1}${getTranslationSuffix()}`
    )
  };

  const subtitle = {
    [IMPORT_CONTACTS]: t(`importContacts.subtitle.step${currentStep + 1}${getTranslationSuffix()}`),
    [IMPORT_CREDENTIALS_DATA]: t(
      `newCredential.subtitle.embeddedStep${currentStep + 1}${getTranslationSuffix()}`
    )
  };

  return (
    <div className="ImportStepPageContainer">
      <div className="TitleContainer">
        <GenericStepsButtons
          steps={getSteps()}
          currentStep={isEmbedded[useCase] ? IMPORT_CREDENTIAL_DATA_STEP : currentStep}
          disableBack={loading}
          disableNext={disableNext}
          loading={loading}
        />
        <WizardTitle title={title[useCase]} subtitle={subtitle[useCase]} />
      </div>
      {renderStep()}
    </div>
  );
};

ImportDataContainer.defaultProps = {
  recipients: null,
  credentialType: {},
  bulkValidator: null,
  loading: false,
  useCase: IMPORT_CONTACTS,
  continueCallback: undefined
};

ImportDataContainer.propTypes = {
  recipients: PropTypes.arrayOf(contactShape),
  credentialType: credentialTypeShape,
  bulkValidator: PropTypes.func,
  onFinish: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]),
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  continueCallback: PropTypes.bool
};

export default ImportDataContainer;
