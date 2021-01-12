import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import img from '../../../../images/success-img.png';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../../../helpers/constants';

const BulkImportSuccess = ({
  contactCreations,
  credentialDataImported,
  continueCallback,
  useCase,
  redirector: { redirectToContacts }
}) => {
  const { t } = useTranslation();

  const successAmount = {
    [IMPORT_CONTACTS]: contactCreations,
    [IMPORT_CREDENTIALS_DATA]: credentialDataImported
  };

  const continueFn = {
    [IMPORT_CONTACTS]: redirectToContacts,
    [IMPORT_CREDENTIALS_DATA]: continueCallback
  };

  return (
    <div className="success-wrapper">
      <div className="success-container">
        <img className="img-success" src={img} alt="" />
        <h1>{t(`bulkImport.importSuccess.${useCase}.title`)}</h1>
        <p>
          {`${successAmount[useCase]} ${t(`bulkImport.importSuccess.${useCase}.uploadedEntities`)}`}
        </p>
        <Button className="theme-secondary" onClick={continueFn[useCase]}>
          {t(`bulkImport.importSuccess.${useCase}.continue`)}
        </Button>
      </div>
    </div>
  );
};

BulkImportSuccess.defaultProps = {
  contactCreations: 0,
  credentialDataImported: 0,
  continueCallback: null
};

BulkImportSuccess.propTypes = {
  contactCreations: PropTypes.number,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func.isRequired }).isRequired,
  credentialDataImported: PropTypes.number,
  continueCallback: PropTypes.func,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default withRedirector(BulkImportSuccess);
