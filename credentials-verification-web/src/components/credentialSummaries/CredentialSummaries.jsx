import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Drawer, message, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import { credentialSummaryShape } from '../../helpers/propShapes';
import DeletionModal from '../common/Organisms/Modals/DeletionModal/DeletionModal';
import CredentialSummaryFilters from './Molecules/Filters/CredentialSummaryFilters';
import CredentialSummaryTable from './Organisms/Tables/CredentialSummaryTable';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryListDetail';
import noGroups from '../../images/noGroups.svg';
import { drawerWidth } from '../../helpers/constants';

const CredentialSummaries = ({
  credentialSummaries,
  setDate,
  setName,
  handleCredentialSummaryDeletion,
  getStudentCredentials,
  onPageChange
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [currentCredentialSummary, setCurrentCredentialSummary] = useState({});

  const closeModal = () => {
    setOpen(false);
    setCurrentCredentialSummary({});
  };

  const modalProps = {
    toDelete: { name: currentCredentialSummary.name, id: currentCredentialSummary.id },
    open,
    closeModal,
    handleDeletion: handleCredentialSummaryDeletion,
    prefix: 'credentialSummary'
  };

  const viewSummary = summary => {
    getStudentCredentials(summary.id)
      .then(credentials => setCurrentCredentialSummary({ ...summary, credentials }))
      .catch(() => {
        message.error(t('errors.errorGetting', { model: 'Credentials' }));
      });
  };

  const showDrawer = !!Object.keys(currentCredentialSummary).length;

  return (
    <div className="Wrapper">
      <Drawer
        title={t('credentialSummary.detail.title')}
        placement="right"
        onClose={() => setCurrentCredentialSummary({})}
        visible={showDrawer}
        width={drawerWidth}
      >
        {showDrawer && <CredentialSummaryDetail {...currentCredentialSummary} />}
      </Drawer>
      <DeletionModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('credentialSummary.title')}</h1>
      </div>
      <CredentialSummaryFilters changeDate={setDate} changeFilter={setName} />
      <Row>
        {credentialSummaries.length ? (
          <CredentialSummaryTable
            setCurrentCredentialSummary={viewSummary}
            credentialSummaries={credentialSummaries}
            onPageChange={onPageChange}
          />
        ) : (
          <EmptyComponent
            photoSrc={noGroups}
            photoAlt="credentialSummary.noCredentialSummaries.photoAlt"
            title="credentialSummary.noCredentialSummaries.title"
          />
        )}
      </Row>
    </div>
  );
};

CredentialSummaries.defaultProps = {
  credentialSummaries: [],
  count: 0,
  offset: 0
};

CredentialSummaries.propTypes = {
  credentialSummaries: PropTypes.arrayOf(credentialSummaryShape),
  count: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  setDate: PropTypes.func.isRequired,
  setName: PropTypes.func.isRequired,
  handleCredentialSummaryDeletion: PropTypes.func.isRequired,
  onPageChange: PropTypes.func.isRequired
};

export default CredentialSummaries;
