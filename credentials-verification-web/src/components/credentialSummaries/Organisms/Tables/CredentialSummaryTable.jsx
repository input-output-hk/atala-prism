import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { credentialSummaryShape } from '../../../../helpers/propShapes';
import genericUserIcon from '../../../../images/genericUserIcon.svg';

const GetActionsButtons = ({ credentialSummary, setCurrentCredentialSummary, openDrawer }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          onClick: () => setCurrentCredentialSummary(credentialSummary),
          className: 'theme-link'
        }}
        buttonText={t('credentialSummary.table.buttons.delete')}
      />
      <CustomButton
        buttonProps={{
          onClick: () => setCurrentCredentialSummary(credentialSummary),
          className: 'theme-link'
        }}
        buttonText={t('credentialSummary.table.buttons.view')}
      />
    </div>
  );
};

GetActionsButtons.propTypes = {
  credentialSummary: PropTypes.shape(credentialSummaryShape).isRequired,
  setCurrentCredentialSummary: PropTypes.func.isRequired,
  openDrawer: PropTypes.func.isRequired
};

const getColumns = (setCurrentCredentialSummary, openDrawer) => {
  const componentName = 'credentialSummary';

  const actionsWidth = 250;
  return [
    {
      key: 'avatar',
      render: ({ icon, fullname }) => (
        <img
          style={{ height: '40px', width: '40px' }}
          src={icon || genericUserIcon}
          alt={`${fullname} icon`}
        />
      )
    },
    {
      key: 'fullname',
      render: ({ user: { fullname } }) => (
        <CellRenderer title="name" componentName={componentName} value={fullname} />
      )
    },
    {
      key: 'email',
      render: ({ user: { email } }) => (
        <CellRenderer title="email" componentName={componentName} value={email} />
      )
    },
    {
      key: 'admissionDate',
      render: ({ date }) => (
        <CellRenderer
          title="date"
          componentName={componentName}
          value={shortBackendDateFormatter(date)}
        />
      )
    },
    {
      key: 'actions',
      width: actionsWidth,
      render: credentialSummary => (
        <GetActionsButtons
          credentialSummary={credentialSummary}
          setCurrentCredentialSummary={setCurrentCredentialSummary}
          openDrawer={openDrawer}
        />
      )
    }
  ];
};

const CredentialSummaryTable = ({
  setCurrentCredentialSummary,
  credentialSummaries,
  onPageChange,
  hasMore,
  openDrawer
}) => {
  const [loading, setLoading] = useState(false);
  const getMoreData = () => {
    setLoading(true);
    onPageChange();
    setLoading(false);
  };

  const tableProps = {
    columns: getColumns(setCurrentCredentialSummary, openDrawer),
    data: credentialSummaries,
    loading,
    hasMore,
    getMoreData
  };

  return (
    <div className="demo-infinite-container">
      <InfiniteScrollTable {...tableProps} />
    </div>
  );
};

CredentialSummaryTable.defaultProps = {
  credentialSummaries: [],
  current: 0,
  total: 0
};

CredentialSummaryTable.propTypes = {
  setOpen: PropTypes.func.isRequired,
  setCurrentCredentialSummary: PropTypes.func.isRequired,
  credentialSummaries: PropTypes.arrayOf(credentialSummaryShape),
  current: PropTypes.number,
  total: PropTypes.number,
  onPageChange: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  openDrawer: PropTypes.func.isRequired
};

export default CredentialSummaryTable;
