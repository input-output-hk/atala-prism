import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../providers/withRedirector';
import ImportTypeSelector from '../importTypeSelector/ImportTypeSelector';
import GenericFooter from '../common/Molecules/GenericFooter/GenericFooter';
import { BULK_IMPORT, MANUAL_IMPORT } from '../../helpers/constants';
import './_style.scss';

const ImportContacts = ({
  redirector: { redirectToContacts, redirectToBulkImport, redirectToManualImport }
}) => {
  const { t } = useTranslation();
  const [selectedMethod, setSelectedMethod] = useState();

  const handleNextStep = () => {
    if (selectedMethod === BULK_IMPORT) redirectToBulkImport();
    if (selectedMethod === MANUAL_IMPORT) redirectToManualImport();
  };

  const footerLabels = {
    previous: t('importContacts.back'),
    next: t('importContacts.next')
  };

  return (
    <div className="ImportContactWrapper">
      <div className="ContentHeader">
        <h1>{t('importContacts.title')}</h1>
        <p>{t('importContacts.info')}</p>
      </div>
      <ImportTypeSelector selected={selectedMethod} onSelect={setSelectedMethod} />
      <GenericFooter
        previous={redirectToContacts}
        next={handleNextStep}
        disableNext={!selectedMethod}
        labels={footerLabels}
      />
    </div>
  );
};

ImportContacts.propTypes = {
  redirector: PropTypes.shape({
    redirectToContacts: PropTypes.func.isRequired,
    redirectToBulkImport: PropTypes.func.isRequired,
    redirectToManualImport: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(ImportContacts);
