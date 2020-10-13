import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import img from '../../../../images/success.svg';

const BulkImportSuccess = ({ contactCreations, redirector: { redirectToContacts } }) => {
  const { t } = useTranslation();

  return (
    <div className="success-container">
      <img className="img-success" src={img} alt="" />
      <h1>{t('bulkImport.importSuccess.title')}</h1>
      <p>{`${contactCreations} ${t('bulkImport.importSuccess.uploadedEntities')}`}</p>
      <Button onClick={redirectToContacts}>{t('bulkImport.importSuccess.continue')}</Button>
    </div>
  );
};

BulkImportSuccess.defaultProps = {
  contactCreations: 0
};

BulkImportSuccess.propTypes = {
  contactCreations: PropTypes.number,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func.isRequired }).isRequired
};

export default withRedirector(BulkImportSuccess);
