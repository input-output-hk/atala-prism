import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { useTranslation } from 'react-i18next';
import img from '../../../../images/success-img.png';
import {
  BULK_IMPORT,
  MANUAL_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../../../helpers/constants';
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

  return (
    <div className="success-wrapper">
      <div className="success-container">
        <img className="img-success" src={img} alt="" />
        <h1>{t(`${importType}.importSuccess.${useCase}.title`)}</h1>
        <p>
          {`${successAmount[useCase]} ${t(
            `${importType}.importSuccess.${useCase}.uploadedEntities`
          )}`}
        </p>
        <Button className="theme-secondary" onClick={continueCallback}>
          {t(`${importType}.importSuccess.${useCase}.continue`)}
        </Button>
      </div>
    </div>
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
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default SuccessPage;
