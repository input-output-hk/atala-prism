import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Drawer, message, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import { credentialSummaryShape } from '../../helpers/propShapes';
import DeletionModal from '../common/Organisms/Modals/DeletionModal/DeletionModal';
import CredentialSummaryFilters from './Molecules/Filters/CredentialSummaryFilters';
import CredentialSummaryTable from './Organisms/Tables/CredentialSummaryTable';
import CredentialSummaryListDetail from '../common/Organisms/Detail/CredentialSummaryListDetail';
import noGroups from '../../images/noGroups.svg';
import { drawerWidth } from '../../helpers/constants';

import './_style.scss';

const CredentialSummaries = ({
  credentialSummaries,
  getSubjectCredentials,
  getCredentialSummaries,
  noSummaries,
  hasMore
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
    handleDeletion: () => {},
    prefix: 'credentialSummary'
  };

  const viewSummary = summary => {
    getSubjectCredentials(summary.id)
      .then(credentials => setCurrentCredentialSummary({ ...summary, credentials }))
      .catch(() => {
        message.error(t('errors.errorGetting', { model: 'Credentials' }));
      });
  };

  const showDrawer = !!Object.keys(currentCredentialSummary).length;
  const title = t('credentialSummary.title');

  return (
    <div className="CredentialSummaries">
      <Drawer
        title={t('credentialSummary.detail.title')}
        placement="right"
        onClose={() => setCurrentCredentialSummary({})}
        visible={showDrawer}
        width={drawerWidth}
      >
        {showDrawer && <CredentialSummaryListDetail {...currentCredentialSummary} />}
      </Drawer>
      <DeletionModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{title}</h1>
      </div>
      <CredentialSummaryFilters getCredentialSummaries={getCredentialSummaries} />
      <Row>
        {credentialSummaries.length ? (
          <CredentialSummaryTable
            setCurrentCredentialSummary={viewSummary}
            credentialSummaries={credentialSummaries}
            getCredentialSummaries={getCredentialSummaries}
            hasMore={hasMore}
          />
        ) : (
          <EmptyComponent photoSrc={noGroups} model={title} isFilter={noSummaries} />
        )}
      </Row>
    </div>
  );
};

CredentialSummaries.defaultProps = {
  credentialSummaries: []
};

CredentialSummaries.propTypes = {
  credentialSummaries: PropTypes.arrayOf(credentialSummaryShape),
  getSubjectCredentials: PropTypes.func.isRequired,
  getCredentialSummaries: PropTypes.func.isRequired,
  noSummaries: PropTypes.bool.isRequired,
  hasMore: PropTypes.bool.isRequired
};

export default CredentialSummaries;
