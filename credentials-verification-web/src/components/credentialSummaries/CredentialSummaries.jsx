import React, { Fragment, useState } from 'react';
import PropTypes from 'prop-types';
import { Drawer, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import { credentialSummaryShape } from '../../helpers/propShapes';
import DeletionModal from '../common/Organisms/Modals/DeletionModal/DeletionModal';
import CredentialSummaryFilters from './Molecules/Filters/CredentialSummaryFilters';
import CredentialSummaryTable from './Organisms/Tables/CredentialSummaryTable';
import CredentialSummaryDetail from './Organisms/Detail/CredentialSummaryDetail';
import noGroups from '../../images/noGroups.svg';
import { drawerWidth } from '../../helpers/constants';

const CredentialSummaries = ({
  credentialSummaries,
  count,
  offset,
  setOffset,
  setDate,
  setName,
  handleCredentialSummaryDeletion
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [currentCredentialSummary, setCurrentCredentialSummary] = useState({});
  const [showDrawer, setShowDrawer] = useState();

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

  console.log('the cred', credentialSummaries);

  return (
    <Fragment>
      <Drawer
        title={t('credentialSummary.detail.title')}
        placement="right"
        onClose={() => setShowDrawer(false)}
        visible={showDrawer}
        width={drawerWidth}
      >
        <CredentialSummaryDetail {...currentCredentialSummary} />
      </Drawer>
      <DeletionModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('credentialSummary.title')}</h1>
      </div>
      <CredentialSummaryFilters changeDate={setDate} changeFilter={setName} />
      <Row>
        {credentialSummaries.length ? (
          <CredentialSummaryTable
            setOpen={setOpen}
            setCurrentCredentialSummary={setCurrentCredentialSummary}
            credentialSummaries={credentialSummaries}
            current={offset + 1}
            total={count}
            offset={offset}
            onPageChange={value => setOffset(value - 1)}
            openDrawer={() => setShowDrawer(true)}
          />
        ) : (
          <EmptyComponent
            photoSrc={noGroups}
            photoAlt="credentialSummary.noCredentialSummaries.photoAlt"
            title="credentialSummary.noCredentialSummaries.title"
          />
        )}
      </Row>
    </Fragment>
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
  handleCredentialSummaryDeletion: PropTypes.func.isRequired
};

export default CredentialSummaries;
