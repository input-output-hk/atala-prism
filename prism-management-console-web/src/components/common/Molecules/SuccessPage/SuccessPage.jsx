import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import SuccessBanner from './SuccessBanner';
import {
  BULK_IMPORT,
  MANUAL_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../../../helpers/constants';
import { importUseCasePropType } from '../../../../helpers/propShapes';
import './_style.scss';

const SuccessPage = ({
  contactCreations,
  credentialDataImported,
  continueCallback,
  importType,
  useCase
}) => {
  const { t } = useTranslation();

  const successAmount = {
    [IMPORT_CONTACTS]: contactCreations,
    [IMPORT_CREDENTIALS_DATA]: credentialDataImported
  };

  const successMessage = t(`${importType}.importSuccess.${useCase}.uploadedEntities`);

  return (
    <SuccessBanner
      title={t(`${importType}.importSuccess.${useCase}.title`)}
      message={`${successAmount[useCase]} ${successMessage}`}
      buttonText={t(`${importType}.importSuccess.${useCase}.continue`)}
      onContinue={continueCallback}
    />
  );
};

SuccessPage.defaultProps = {
  contactCreations: 0,
  credentialDataImported: 0,
  continueCallback: null
};

SuccessPage.propTypes = {
  contactCreations: PropTypes.number,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func.isRequired }).isRequired,
  credentialDataImported: PropTypes.number,
  continueCallback: PropTypes.func,
  importType: PropTypes.oneOf([MANUAL_IMPORT, BULK_IMPORT]).isRequired,
  useCase: importUseCasePropType.isRequired
};

export default SuccessPage;
