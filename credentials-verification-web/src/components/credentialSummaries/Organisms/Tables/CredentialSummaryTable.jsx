import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { CREDENTIAL_SUMMARY_PAGE_SIZE, AVATAR_WIDTH } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { credentialSummaryShape } from '../../../../helpers/propShapes';

const GetActionsButtons = ({ credentialSummary, setCurrentCredentialSummary, openDrawer }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          onClick: setCurrentCredentialSummary,
          className: 'theme-link'
        }}
        buttonText={t('credentialSummary.table.buttons.delete')}
      />
      <CustomButton
        buttonProps={{
          onClick: () => {
            setCurrentCredentialSummary(credentialSummary);
            openDrawer();
          },
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
      key: 'icon',
      width: AVATAR_WIDTH,
      render: ({ user: { icon, name } }) => (
        <img style={{ height: '40px', width: '40px' }} src={icon} alt={`${name} icon`} />
      )
    },
    {
      key: 'name',
      render: ({ user: { name } }) => (
        <CellRenderer title="name" componentName={componentName} value={name} />
      )
    },
    {
      key: 'date',
      render: ({ date }) => (
        <CellRenderer title="date" componentName={componentName} value={shortDateFormatter(date)} />
      )
    },
    {
      key: 'totalCredentials',
      render: ({ user: { transactions } }) => (
        <CellRenderer
          title="totalCredentials"
          componentName={componentName}
          value={transactions.length}
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
  current,
  total,
  onPageChange,
  openDrawer
}) => {
  const tableProps = {
    columns: getColumns(setCurrentCredentialSummary, openDrawer),
    data: credentialSummaries,
    current,
    total,
    defaultPageSize: CREDENTIAL_SUMMARY_PAGE_SIZE,
    onChange: onPageChange
  };

  return <PaginatedTable {...tableProps} />;
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
  onPageChange: PropTypes.func.isRequired
};

export default CredentialSummaryTable;
